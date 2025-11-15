package com.bes.jira.plugins.oauth2bridge.rest;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.bes.jira.plugins.oauth2bridge.api.MyPluginComponent;
import com.bes.jira.plugins.oauth2bridge.store.StateCache;
import com.bes.jira.plugins.oauth2bridge.util.OAuthStateGenerator;
import com.bes.jira.plugins.oauth2bridge.util.Url;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/oauth")
@AnonymousAllowed
public class Oauth {
    @ComponentImport
    private final ApplicationProperties applicationProperties;
    private final StateCache stateCache;

    // 使用 @Inject 注入依赖项
    @Inject
    public Oauth(final ApplicationProperties applicationProperties, StateCache stateCache) {
        this.applicationProperties = applicationProperties;
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
        // 获取当前的oauth2配置
        String oauth2AccessTokenUrl = "http://localhost:8888/oauth/token";
        String userAuthorizationUri = "//localhost:8888/oauth/authorize";
        String oauth2ClientId = "jira";
        String oauth2Scope = "";
        // todo 生成state，获取oauth2的登录地址并设置redirect_uri
        String oauth2State = OAuthStateGenerator.generateState();
        stateCache.storeState(oauth2State, redirectUri);

        String redirectBaseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE) + "/rest/oauth2bridge/1.0/oauth/callback";
        Map<String, String> callbackQueryParams = new HashMap<>();
        callbackQueryParams.put("redirect_uri", redirectUri);
        callbackQueryParams.put("state", state);
        System.out.println(redirectUri + "+" + state);
        String oauth2RedirectUrl = Url.buildQueryUrl(redirectBaseUrl, callbackQueryParams);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("response_type", "code");
        queryParams.put("client_id", oauth2ClientId);
        queryParams.put("redirect_uri", oauth2RedirectUrl);
        queryParams.put("state", oauth2State);
        queryParams.put("scope", oauth2Scope);
        String oauth2LoginUrl = Url.buildQueryUrl(oauth2AccessTokenUrl, queryParams);
        return Response.seeOther(URI.create(oauth2LoginUrl)).build();
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
        String storeRedirectUri = stateCache.retrieveAndRemoveState(state);
        if (storeRedirectUri == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid or expired state").build();
        }
        // 1) 用 code 换 token
        // 2) 用 token 获取 userInfo
        // 3) 用 userInfo 获取 jiraToken
        // 4) 用 jiraToken 响应到回调地址
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("code", "");
        URI uri = URI.create(redirectUri);
        String path = uri.getPath();
        String oauth2LoginUrl = Url.buildQueryUrl(path, queryParams) + "&" + uri.getQuery();
        return Response.seeOther(URI.create(oauth2LoginUrl)).build();
    }
}
