package com.bes.jira.plugins.oauth2bridge.servlet.filter;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
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

    @Inject
    public Oauth2BridgeServletFilter(Oauth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.info("Oauth2BridgeServletFilter doFilter With Bearer");
            String token = authHeader.substring("Bearer ".length());
            try {
                UserInfo userInfo = oauth2Service.getUserInfo(token);
                ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(userInfo.getName());
                if (user != null) {
                    // 设置用户到请求上下文
                    JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
                    authContext.setLoggedInUser(user);
                } else {
                    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found in Jira");
                }
            } catch (InvalidParameterException e) {
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
