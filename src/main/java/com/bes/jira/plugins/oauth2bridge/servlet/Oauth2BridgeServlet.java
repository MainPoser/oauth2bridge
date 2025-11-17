package com.bes.jira.plugins.oauth2bridge.servlet;

import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import com.bes.jira.plugins.oauth2bridge.util.RawHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
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

    // 构造函数注入依赖
    @Inject
    public Oauth2BridgeServlet(
            UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer renderer) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
    }

    // ----------------------------------------------------
    // GET 请求 (页面显示)
    // ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        // 渲染模板
        renderer.render("templates/oauth2-bridge.vm", resp.getWriter());
    }
}