package com.bes.jira.plugins.oauth2bridge.servlet;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Named
public class Oauth2BridgeServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeServlet.class);
    @ComponentImport // 注入 Atlassian 提供的服务
    private final TemplateRenderer renderer;

    // 构造函数注入依赖
    @Inject
    public Oauth2BridgeServlet(TemplateRenderer renderer) {
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