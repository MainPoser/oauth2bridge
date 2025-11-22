package com.bes.jira.plugins.oauth2bridge.servlet.filter;

import com.bes.jira.plugins.oauth2bridge.cache.TokenCache;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2Service;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Named
public class RestServletFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RestServletFilter.class);

    private final SettingService settingService;
    private final TokenCache tokenCache;

    @Inject
    public RestServletFilter(SettingService settingService, TokenCache tokenCache) {
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
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        // --- 追踪开始: 记录请求路径，便于调试 /* 模式 ---
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String servletPath = httpServletRequest.getServletPath();
        log.debug(">> [OAuth2 Rest Filter] Request START for path: {}", servletPath);
        // 当请求的是当前插件的rest接口时
        boolean isBasicAuthProxyPath = servletPath.startsWith("/rest/oauth2bridge");
        // 如果是目标路径 且 带有 Basic 头部
        if (isBasicAuthProxyPath && authHeader != null && authHeader.startsWith("Basic ")) {
            log.debug(">> [Basic Rest Filter] Found Basic Auth header on proxy path: {}. Hiding from JIRA.", servletPath);

            // 1. 将原始头部存储在 Request 属性中，以便下游 REST 资源获取
            httpServletRequest.setAttribute("OriginalAuthorizationHeader", authHeader);

            // 2. 创建 Wrapper，隐藏 Authorization 头部，防止 JIRA 拦截
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(httpServletRequest) {
                @Override
                public String getHeader(String name) {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                        return null; // 隐藏 Basic 头部
                    }
                    return super.getHeader(name);
                }

                // 还需要覆盖 getHeaders/getHeaderNames，但简化起见，先只覆盖 getHeader
                @Override
                public Enumeration getHeaders(String name) {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                        // 返回一个空的 Enumeration，表示没有找到该头部的值。
                        return Collections.emptyEnumeration();
                    }
                    // 必须使用泛型 <String>
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration getHeaderNames() {
                    // 1. 获取原始的头部名称列表
                    List<String> names = Collections.list(super.getHeaderNames());

                    // 2. 从列表中移除 Authorization (不区分大小写)
                    names.removeIf(HttpHeaders.AUTHORIZATION::equalsIgnoreCase);

                    // 3. 返回新的 Enumeration
                    return Collections.enumeration(names);
                }
            };

            // 3. 将请求引用替换为被包装的请求
            // 这样后续的 JIRA 认证和您自己的 Bearer 逻辑就看不到 Basic 头部了
            httpServletRequest = wrappedRequest;
        }
        // --- Basic 认证头部处理逻辑 结束 ---

        // --- 过滤器链执行和清理 ---
        log.debug(">> [Filter Chain] Passing request to next filter.");
        filterChain.doFilter(httpServletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        log.info("[OAuth2 Rest Filter] Filter destroyed.");
    }
}
