package com.bes.jira.plugins.oauth2bridge.rest;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.bes.jira.plugins.oauth2bridge.store.StateCache;
import com.bes.jira.plugins.oauth2bridge.util.OAuthStateGenerator;
import com.bes.jira.plugins.oauth2bridge.util.Url;

import javax.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Path("/oauth")
@AnonymousAllowed
public class Oauth {
    @ComponentImport
    private final ApplicationProperties applicationProperties;
    @ComponentImport
    private final UserManager userManager;
    private final StateCache stateCache;

    // 使用 @Inject 注入依赖项
    @Inject
    public Oauth(final ApplicationProperties applicationProperties, StateCache stateCache, UserManager userManager) {
        this.applicationProperties = applicationProperties;
        this.stateCache = stateCache;
        this.userManager = userManager;
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
        try {
            // 1️⃣ 用 HttpURLConnection 换 token
            String tokenResp = postToken(code);

            // 简单解析 access_token (假设返回 {"access_token":"xxx"})
            String accessToken = parseAccessToken(tokenResp);

            // 2️⃣ 获取用户信息
            String username = fetchUsername(accessToken);

            // 3️⃣ 找到 Jira 用户
            ApplicationUser jiraUser = userManager.getUserByName(username);
            if (jiraUser == null) {
                return Response.status(401).entity("No matching Jira user").build();
            }

            // 4️⃣ 自动登录
            // LoginManager loginManager = ComponentAccessor.getComponent(DefaultAuthenticator.class);
            // loginManager.login(request, response, jiraUser);
            // 5️⃣ 302 重定向
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("code", "");
            URI uri = URI.create(redirectUri);
            String path = uri.getPath();
            String oauth2LoginUrl = Url.buildQueryUrl(path, queryParams) + "&" + uri.getQuery();
            return Response.seeOther(URI.create(oauth2LoginUrl)).build();


        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    private String postToken(String code) throws IOException {
        URL url = new URL("https://oauth-server.com/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String params = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode("https://jira.yourcompany.com/plugins/servlet/oauth2/callback", "UTF-8")
                + "&client_id=YOUR_CLIENT_ID"
                + "&client_secret=YOUR_CLIENT_SECRET";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes("UTF-8"));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
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

    private String fetchUsername(String accessToken) throws IOException {
        URL url = new URL("https://oauth-server.com/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String resp = sb.toString();

            // 假设返回 {"username":"jirauser"}，简单解析
            int i = resp.indexOf("\"username\":\"");
            int start = i + 12;
            int end = resp.indexOf("\"", start);
            return resp.substring(start, end);
        }
    }
}
