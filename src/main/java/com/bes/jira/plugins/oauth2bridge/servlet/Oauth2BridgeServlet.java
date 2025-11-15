package com.bes.jira.plugins.oauth2bridge.servlet;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Named
public class Oauth2BridgeServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeServlet.class);
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    @ComponentImport // 注入 Atlassian 提供的服务
    private final TemplateRenderer renderer;
    private final Oauth2BridgeConfigService configService;

    // 构造函数注入依赖
    @Inject
    public Oauth2BridgeServlet(
            UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer renderer,
            Oauth2BridgeConfigService configService) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
        this.configService = configService;
    }

    // ----------------------------------------------------
    // GET 请求 (页面显示)
    // ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = userManager.getRemoteUsername(req);
        if (username == null || !userManager.isSystemAdmin(username)) {
            redirectToLogin(req, resp);
            return;
        }
        resp.setContentType("text/html;charset=UTF-8");

        // 1. 读取配置
        String clientId = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID);
        String clientSecret = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET);
        String authorizationEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT);
        String tokenEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT);
        String userInfoEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);
        log.info("Loaded config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);
        // 2. 准备 context
        Map<String, Object> context = new HashMap<>();
        context.put("clientId", clientId);
        context.put("clientSecret", clientSecret);
        context.put("authorizationEndpoint", authorizationEndpoint);
        context.put("tokenEndpoint", tokenEndpoint);
        context.put("userInfoEndpoint", userInfoEndpoint);
        // 用于 Velocity 模板生成 POST 目标 URL
        context.put("actionUrl", req.getContextPath() + req.getServletPath());

        // 3. 渲染模板
        renderer.render("templates/oauth2-bridge-config.vm", context, resp.getWriter());
    }

    // ----------------------------------------------------
    // POST 请求 (表单提交)
    // ----------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 获取表单值 (Servlet 方式)
        String clientId = req.getParameter("clientId");
        String clientSecret = req.getParameter("clientSecret");
        String authorizationEndpoint = req.getParameter("authorizationEndpoint");
        String tokenEndpoint = req.getParameter("tokenEndpoint");
        String userInfoEndpoint = req.getParameter("userInfoEndpoint");

        log.info("Saving config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);

        // 2. 校验和保存
        if (clientId == null || clientId.trim().isEmpty()) {
            // 简单错误处理：可以重新调用 doGet 渲染页面并添加错误信息
            // 复杂的错误处理推荐使用 XWork Action
            resp.sendRedirect(req.getContextPath() + req.getServletPath() + "?error=true");
            return;
        }

        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID, clientId.trim());
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET, clientSecret.trim());
        configService.saveConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT, authorizationEndpoint.trim());
        configService.saveConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT, tokenEndpoint.trim());
        configService.saveConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint.trim());

        // 3. 重定向到自身，显示成功信息
        resp.sendRedirect(req.getContextPath() + req.getServletPath() + "?success=true");
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    // 遮掩敏感信息
    private String mask(String s) {
        if (s == null || s.length() <= 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}