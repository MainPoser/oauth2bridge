package com.bes.jira.plugins.oauth2bridge.servlet.filter;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.bes.jira.plugins.oauth2bridge.cache.TokenCache;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import com.bes.jira.plugins.oauth2bridge.model.IntrospectionResponse;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2Service;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;

@Named
public class Oauth2ServletFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(Oauth2ServletFilter.class);

    private final Oauth2Service oauth2Service;
    private final SettingService settingService;
    private final TokenCache tokenCache;

    @Inject
    public Oauth2ServletFilter(Oauth2Service oauth2Service, SettingService settingService, TokenCache tokenCache) {
        this.oauth2Service = oauth2Service;
        this.settingService = settingService;
        this.tokenCache = tokenCache;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化缓存
        long sessionTimeoutSec = this.settingService.getSetting().getSessionTimeoutSec();
        this.tokenCache.init(sessionTimeoutSec);
        log.info("[OAuth2 Filter] TokenCache initialized with session timeout: {} seconds.", sessionTimeoutSec);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser userToSet = null;

        // --- 追踪开始: 记录请求路径，便于调试 /* 模式 ---
        String servletPath = httpRequest.getServletPath();
        log.debug(">> [OAuth2 Filter] Request START for path: {}", servletPath);
        // 当请求的是当前插件的rest接口时
        boolean isBasicAuthProxyPath = servletPath.startsWith("/rest/oauth2bridge");

        // 只有带了Authorization的才认为是oauth2要介入的
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ") || isBasicAuthProxyPath) {
            log.debug("<< [OAuth2 Filter] No Bearer token found, exclude matched ({}), or no Bearer prefix. Bypassing authentication.", isBasicAuthProxyPath);
            // 如果没有Bearer Token，直接放行，进入下一个 try/finally block
        } else {
            // --- Bearer Token 流程开始 ---
            String accessToken = authHeader.substring("Bearer ".length());
            String maskedToken = StringUtils.left(accessToken, 8) + "..."; // 关键：脱敏

            log.debug(">> [OAuth2 Filter] Bearer token authentication initiated. Masked Token: {}", maskedToken);

            // 确认当前的tokenCache是否和配置一致，如果不一致，则需要重建缓存
            long currentTimeout = settingService.getSetting().getSessionTimeoutSec();
            if (currentTimeout != tokenCache.getDuration()) {
                // INFO: 记录缓存重建动作
                log.info("[OAuth2 Filter] TokenCache duration changed from {}s to {}s. Rebuilding cache.", tokenCache.getDuration(), currentTimeout);
                tokenCache.rebuildCacheWithNewExpire(currentTimeout);
            }
            try {
                // 1. 尝试在缓存查找
                userToSet = tokenCache.get(accessToken);
                if (userToSet != null) {
                    log.debug(">> [Cache HIT] Token {} found in cache for user: {}", maskedToken, userToSet.getName());
                } else {
                    log.debug(">> [Cache MISS] Token {} not found in cache. Proceeding to remote introspection.", maskedToken);

                    // 缓存找不到再去oauth2服务器获取
                    IntrospectionResponse introspectionRes = oauth2Service.introspection(accessToken);
                    if (introspectionRes.isSuccess()) {
                        // 正常处理 Introspection
                        Introspection introspection = introspectionRes.getIntrospection();
                        // 增强: 打印Introspection的关键信息
                        log.debug(">> [Introspection SUCCESS] Sub: {}, Active: {}", introspection.getSub(), introspection.isActive());

                        // JIRA 用户查找
                        ApplicationUser tempUser = ComponentAccessor.getUserManager().getUserByName(introspection.getSub());

                        if (tempUser == null) {
                            // WARN: 用户的 Sub 存在，但 JIRA 不认识
                            log.warn("!! [User Lookup FAILED] User '{}' from Introspection (Token: {}) not found in Jira user manager.",
                                    introspection.getSub(), maskedToken);
                            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found in Jira");
                            return; // 认证失败，中断请求链
                        }
                        userToSet = tempUser; // 设置最终的用户

                        log.info(">> [User Lookup SUCCESS] Found Jira user: {}. Caching token {}.", userToSet.getName(), maskedToken);
                        tokenCache.put(accessToken, userToSet);
                    } else {
                        // ERROR: Introspection 远程失败
                        log.error("<< [Introspection FAILED] Remote validation failed (Status: {}, Error: {}). Masked Token: {}.",
                                introspectionRes.getStatusCode(), introspectionRes.getErrorMessage(), maskedToken);
                        httpResponse.sendError(introspectionRes.getStatusCode(), introspectionRes.getErrorMessage());
                        return; // 认证失败，中断请求链
                    }
                }

                // 2. 成功获取用户后，设置用户到请求上下文
                log.debug(">> [Context Set] Setting user '{}' to JiraAuthenticationContext.", userToSet.getName());
                authContext.setLoggedInUser(userToSet);

            } catch (InvalidParameterException e) {
                // WARN: token 格式无效等情况，认证失败
                log.warn("!! [Auth Failed] Invalid Bearer token format/parameter. Masked Token: {}. Error: {}", maskedToken, e.getMessage());
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: Invalid token parameter");
                return; // 认证失败，中断请求链
            } catch (URISyntaxException e) {
                // ERROR: 配置中的 URL 导致无法执行请求
                log.error("!! [Auth Failed] Configuration error: Introspection URL is invalid. Masked Token: {}.", maskedToken, e);
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication internal error: Configuration issue");
                return;
            } catch (IOException e) {
                // ERROR: 网络 I/O 错误
                log.error("!! [Auth Failed] Network/I/O error during introspection. Masked Token: {}.", maskedToken, e);
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication internal error: Network issue");
                return;
            }
        }

        // --- 过滤器链执行和清理 ---
        try {
            // 3. 继续处理过滤器链
            log.debug(">> [Filter Chain] Passing request to next filter.");
            filterChain.doFilter(httpRequest, servletResponse);
        } finally {
            // 4. 【关键修正】如果我们在上面设置了用户，请求完成后必须清理！
            if (userToSet != null && authContext != null) {
                // 避免清空其他过滤器设置的用户，只清理自己设置的
                ApplicationUser currentUser = authContext.getLoggedInUser();
                if (currentUser != null && currentUser.equals(userToSet)) {
                    authContext.clearLoggedInUser();
                    log.debug("<< [Context Clear] Cleared user '{}' from JiraAuthenticationContext.", userToSet.getName());
                } else if (currentUser != null) {
                    log.debug("<< [Context Skipped] Did not clear user '{}' as it was changed by downstream filters.", currentUser.getName());
                } else {
                    log.debug("<< [Context Skipped] Context was already cleared by downstream filters.");
                }
            }
            log.debug("<< [OAuth2 Filter] Request END for path: {}", servletPath);
        }
    }

    @Override
    public void destroy() {
        log.info("[OAuth2 Filter] Filter destroyed.");
    }
}
