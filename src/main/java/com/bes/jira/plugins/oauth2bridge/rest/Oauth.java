package com.bes.jira.plugins.oauth2bridge.rest;

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
import com.bes.jira.plugins.oauth2bridge.store.StateCache;
import com.bes.jira.plugins.oauth2bridge.util.OAuthStateGenerator;
import com.bes.jira.plugins.oauth2bridge.util.Url;

import javax.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/oauth")
@AnonymousAllowed
public class Oauth {
    @ComponentImport
    private final ApplicationProperties applicationProperties;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    private final Oauth2BridgeConfigService configService;
    private final Oauth2Service oauth2Service;
    private final StateCache stateCache;

    // 使用 @Inject 注入依赖项
    @Inject
    public Oauth(final ApplicationProperties applicationProperties, UserManager userManager,
                 LoginUriProvider loginUriProvider, Oauth2BridgeConfigService configService,
                 Oauth2Service oauth2Service, StateCache stateCache) {
        this.applicationProperties = applicationProperties;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.configService = configService;
        this.oauth2Service = oauth2Service;
        this.stateCache = stateCache;
    }

    /**
     * GET 方法示例
     * 访问路径: /rest/oauth2bridge/1.0/oauth/login
     */
    @GET
    @Path("/login")
    public Response login(@QueryParam("redirect_uri") String redirectUri, @QueryParam("state") String state) {
        if (redirectUri == null || redirectUri.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing redirect_uri").build();
        }
        // todo 生成state，获取oauth2的登录地址并设置redirect_uri
        String oauth2State = OAuthStateGenerator.generateState();
        stateCache.storeState(oauth2State, redirectUri);

        String redirectBaseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE) + "/rest/oauth2bridge/1.0/oauth/callback";
        Map<String, String> callbackQueryParams = new HashMap<>();
        callbackQueryParams.put("redirect_uri", redirectUri);
        callbackQueryParams.put("state", state);
        String oauth2RedirectUrl = Url.buildQueryUrl(redirectBaseUrl, callbackQueryParams);
        return Response.seeOther(URI.create(oauth2Service.buildAuthorizationUrl(oauth2State, "", oauth2RedirectUrl))).build();
    }

    /**
     * GET 方法示例
     * 访问路径: /rest/oauth2bridge/1.0/oauth/login
     */
    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state, @QueryParam("redirect_uri") String redirectUri) {
        if (state == null || code == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing state or code").build();
        }
        // 验证state是否有效
        if (stateCache.retrieveAndRemoveState(state) == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid or expired state").build();
        }
        try {
            // 1️⃣ 用 HttpURLConnection 换 token
            TokenResponse tokenResponse = oauth2Service.requestToken(code);

            // 2️⃣ 获取用户信息
            UserInfo userInfo = oauth2Service.getUserInfo(tokenResponse.getAccessToken());

            // 3️⃣ 找到 Jira 用户
//            ApplicationUser jiraUser = userManager.getUserByName(username);
//            if (jiraUser == null) {
//                return Response.status(401).entity("No matching Jira user").build();
//            }

            // 4️⃣ 自动登录
            // LoginManager loginManager = ComponentAccessor.getComponent(DefaultAuthenticator.class);
            // loginManager.login(request, response, jiraUser);
            // 5️⃣ 302 重定向
            URI uri = URI.create(redirectUri);
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("code", "");
            return Response.seeOther(URI.create(Url.buildQueryUrl(uri.getPath(), queryParams) + "&" + uri.getQuery())).build();


        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    private String parseAccessToken(String resp) {
        // 简单解析 {"access_token":"xxxx"}，无依赖
        int i = resp.indexOf("\"access_token\":\"");
        if (i < 0) return null;
        int start = i + 16;
        int end = resp.indexOf("\"", start);
        return resp.substring(start, end);
    }
}
