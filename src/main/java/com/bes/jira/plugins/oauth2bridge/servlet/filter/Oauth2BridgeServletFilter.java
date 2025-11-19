package com.bes.jira.plugins.oauth2bridge.servlet.filter;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.bes.jira.plugins.oauth2bridge.cache.TokenCache;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidParameterException;

@Named
public class Oauth2BridgeServletFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeServletFilter.class);

    private final Oauth2Service oauth2Service;
    private final Oauth2BridgeConfigService oauth2BridgeConfigService;
    private TokenCache tokenCache;

    @Inject
    public Oauth2BridgeServletFilter(Oauth2Service oauth2Service, Oauth2BridgeConfigService oauth2BridgeConfigService) {
        this.oauth2Service = oauth2Service;
        this.oauth2BridgeConfigService = oauth2BridgeConfigService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String sessionTimeoutSecStr = this.oauth2BridgeConfigService.getConfig(Oauth2BridgeConfigService.KEY_SESSION_TIMEOUT_SEC);
        long sessionTimeoutSec = 30 * 60;
        try {
            sessionTimeoutSec = Long.parseLong(sessionTimeoutSecStr);
        } catch (NumberFormatException e) {
            log.warn("Parse sessionTimeoutSec failed, use default");
        }
        this.tokenCache = new TokenCache(sessionTimeoutSec);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser userToSet = null;

        // --- 追踪开始: 记录请求路径，便于调试 /* 模式 ---
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        log.debug(">> [OAuth2 Filter] Request START for path: {}", path);
        // ----------------------------------------------------

        // 只有带了Authorization的才认为是oauth2要介入的
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("<< [OAuth2 Filter] No Bearer token found. Bypassing authentication.");
            // 如果没有Bearer Token，直接放行，进入下一个 try/finally block
        } else {
            // --- Bearer Token 流程开始 ---
            String accessToken = authHeader.substring("Bearer ".length());
            try {
                // 1. 尝试在缓存查找
                userToSet = tokenCache.get(accessToken);
                if (userToSet != null) {
                    log.debug(">> [Cache HIT] Token found in cache for user: {}", userToSet.getName());
                } else {
                    log.debug(">> [Cache MISS] Token not found in cache. Proceeding to remote introspection.");

                    // 缓存找不到再去oauth2服务器获取
                    Introspection introspection = oauth2Service.introspection(accessToken);

                    // 增强: 打印Introspection的关键信息
                    log.debug(">> [Introspection] Sub: {}, Active: {}", introspection.getSub(), introspection.isActive());

                    // 校验 token active 状态
                    if (!introspection.isActive()) {
                        log.warn("!! [Introspection] Token is marked as inactive or expired.");
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is inactive or expired.");
                        return; // 认证失败，中断请求链
                    }

                    userToSet = ComponentAccessor.getUserManager().getUserByName(introspection.getSub());
                    if (userToSet == null) {
                        log.warn("!! [User Lookup] User '{}' from Introspection not found in Jira user manager.", introspection.getSub());
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found in Jira");
                        return; // 认证失败，中断请求链
                    }

                    log.debug(">> [User Lookup SUCCESS] Found Jira user: {}. Caching token.", userToSet.getName());
                    tokenCache.put(accessToken, userToSet);
                }

                // 2. 成功获取用户后，设置用户到请求上下文
                log.debug(">> [Context Set] Setting user '{}' to JiraAuthenticationContext.", userToSet.getName());
                authContext.setLoggedInUser(userToSet);

            } catch (InvalidParameterException e) {
                // token无效等情况，认证失败
                log.info("!! [Auth Failed] Bearer token exception: {}", e.getMessage());
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: " + e.getMessage());
                return; // 认证失败，中断请求链
            }
            // --- Bearer Token 流程结束 ---
        }

        // --- 过滤器链执行和清理 ---
        try {
            // 3. 继续处理过滤器链
            log.debug(">> [Filter Chain] Passing request to next filter.");
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 4. 【关键修正】如果我们在上面设置了用户，请求完成后必须清理！
            if (userToSet != null && authContext != null) {
                // 避免清空其他过滤器设置的用户，只清理自己设置的
                if (authContext.getLoggedInUser() != null && authContext.getLoggedInUser().equals(userToSet)) {
                    authContext.clearLoggedInUser();
                    log.debug("<< [Context Clear] Cleared user '{}' from JiraAuthenticationContext.", userToSet.getName());
                } else if (authContext.getLoggedInUser() != null) {
                    log.debug("<< [Context Skipped] Did not clear user '{}' as it was changed by downstream filters.", authContext.getLoggedInUser().getName());
                } else {
                    log.debug("<< [Context Skipped] Context was already cleared by downstream filters.");
                }
            }
            log.debug("<< [OAuth2 Filter] Request END for path: {}", path);
        }
    }

    @Override
    public void destroy() {
    }
}
