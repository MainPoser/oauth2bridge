package com.bes.jira.plugins.oauth2bridge.servlet.filter;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.bes.jira.plugins.oauth2bridge.cache.TokenCache;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
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

        // 只有带了Authorization的才认为是oauth2要介入的
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Oauth2BridgeServletFilter doFilter With Bearer");
            String accessToken = authHeader.substring("Bearer ".length());
            try {
                // 1. 尝试在缓存查找
                ApplicationUser user = tokenCache.get(accessToken);
                if (user == null) {
                    // 缓存找不到再去oauth2服务器获取,同时能校验access_token是否有效
                    UserInfo userInfo = oauth2Service.getUserInfo(accessToken);
                    user = ComponentAccessor.getUserManager().getUserByName(userInfo.getName());
                    if (user == null) {
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found in Jira");
                        return;
                    }
                    tokenCache.put(accessToken, user);
                }
                // 设置用户到请求上下文
                JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
                authContext.setLoggedInUser(user);
            } catch (InvalidParameterException e) {
                // token无效等情况，认证失败
                log.info("Oauth2BridgeServletFilter doFilter With Bearer exception: {}", e.getMessage());
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
