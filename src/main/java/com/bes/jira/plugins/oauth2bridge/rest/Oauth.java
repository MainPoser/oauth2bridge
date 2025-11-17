package com.bes.jira.plugins.oauth2bridge.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.bes.jira.plugins.oauth2bridge.model.TokenResponse;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2Service;
import com.bes.jira.plugins.oauth2bridge.servlet.filter.Oauth2BridgeServletFilter;
import com.bes.jira.plugins.oauth2bridge.store.StateCache;
import com.bes.jira.plugins.oauth2bridge.util.OAuthStateGenerator;
import com.bes.jira.plugins.oauth2bridge.util.Url;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Path("/oauth")
@AnonymousAllowed
public class Oauth {
    private static final Logger log = LoggerFactory.getLogger(Oauth.class);
    @ComponentImport
    private final ApplicationProperties applicationProperties;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    private final Oauth2BridgeConfigService configService;
    private final Oauth2Service oauth2Service;
    private final StateCache stateCache;

    // 使用 @Inject 注入依赖项
    @Inject
    public Oauth(final ApplicationProperties applicationProperties,
                 LoginUriProvider loginUriProvider, Oauth2BridgeConfigService configService,
                 Oauth2Service oauth2Service, StateCache stateCache) {
        this.applicationProperties = applicationProperties;
        this.loginUriProvider = loginUriProvider;
        this.configService = configService;
        this.oauth2Service = oauth2Service;
        this.stateCache = stateCache;
    }

    /**
     * 授权端点：跳转到真实的oauth2服务器
     * 访问路径: /rest/oauth2bridge/1.0/oauth/authorize
     */
    @GET
    @Path("/authorize")
    public Response authorize(@Context UriInfo uriInfo) {
        // 所有的请求参数
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        // 2. 定义插件的回调地址
        // 必须使用绝对 URL
        String pluginCallbackUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
                + "/rest/oauth2bridge/1.0/oauth/callback";

        // 3. 生成 OAuth 2.0 状态 (state)
        String oauth2State = OAuthStateGenerator.generateState();
        // 4. 将【原始请求的 redirect_uri】存入 stateCache 供回调时使用
        // 假设原始请求包含一个 'redirect_uri' 指向最终的应用地址
        String finalRedirectUri = queryParams.getFirst("redirect_uri");
        if (finalRedirectUri != null) {
            stateCache.storeState(oauth2State, finalRedirectUri);
        }

        // 5. 准备要转发给 IdP 的参数集合 (MutableMap 是不可变的，我们创建一个新的可修改的 Map)
        // 复制所有原始参数
        MultivaluedMap<String, String> paramsForIdP = new MultivaluedMapImpl(queryParams);

        // 6. 核心修改：设置正确的 state 和 redirect_uri
        // a) 设置您的插件生成的 state 参数
        paramsForIdP.put("state", Collections.singletonList(oauth2State));

        // b) 覆盖 redirect_uri 为您的插件回调地址
        paramsForIdP.put("redirect_uri", Collections.singletonList(pluginCallbackUrl));

        // 7. 构建最终重定向到 oauth2 的 URL
        String authorizationUrl = oauth2Service.buildAuthorizationUrl(paramsForIdP);
        return Response.seeOther(URI.create(authorizationUrl)).build();
    }

    /**
     * GET 方法示例
     * 访问路径: /rest/oauth2bridge/1.0/oauth/callback
     */
    @GET
    @Path("/callback")
    public Response callback(@Context UriInfo uriInfo) {
        // 1. 获取授权码和状态
        String authCode = uriInfo.getQueryParameters().getFirst("code");
        String state = uriInfo.getQueryParameters().getFirst("state");
        String error = uriInfo.getQueryParameters().getFirst("error");
        // 检查是否有错误 (如用户拒绝授权)
        if (error != null) {
            log.error("OAuth authorization failed with error: {}", error);
            // 这里应该重定向到用户友好的错误页面
            return Response.serverError().entity("Authorization failed: " + error).build();
        }
        // 2. 验证 state 并获取最终重定向地址
        if (state == null || authCode == null) {
            return Response.serverError().entity("Missing 'code' or 'state' parameter in callback.").build();
        }
        String finalRedirectUri = stateCache.retrieveAndRemoveState(state);
        if (finalRedirectUri == null) {
            log.error("Invalid or expired state received: {}", state);
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid OAuth state.").build();
        }
        String finalUrl = finalRedirectUri;
        String separator = finalRedirectUri.contains("?") ? "&" : "?";
        finalUrl += separator + "code=" + authCode;
        return Response.seeOther(URI.create(finalUrl)).build();
    }

    /**
     * Token端点：获取token
     * 访问路径: /rest/oauth2bridge/1.0/oauth/token
     */
    @GET
    @Path("/token")
    public Response token(@Context UriInfo uriInfo) {
        try {
            // 用 code 换 token
            TokenResponse tokenResponse = oauth2Service.requestToken(uriInfo.getQueryParameters());
            // 返回token信息
            return Response.ok(tokenResponse).build();
        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }
}
