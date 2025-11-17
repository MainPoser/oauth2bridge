package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.model.TokenResponse;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Named;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Named
public class Oauth2Service {

    private final Oauth2BridgeConfigService configService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Oauth2Service(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    /**
     * 构建授权 URL
     */
    public String buildAuthorizationUrl(String state, String scope, String redirectUri) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(configService.getConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT)).append("?")
                    .append("response_type=code")
                    .append("&client_id=").append(URLEncoder.encode(configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID), "UTF-8"))
                    .append("&client_secret=").append(URLEncoder.encode(configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET), "UTF-8"))
                    .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
            if (scope != null) {
                sb.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
            }
            if (state != null) {
                sb.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
            }

            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用 code 换 token（返回 JSON 字符串）
     */
    public TokenResponse requestToken(String code) throws IOException {
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod(configService.getConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT));
        NameValuePair[] params = new NameValuePair[]{
                new NameValuePair("grant_type", "authorization_code"),
                new NameValuePair("code", code),
                new NameValuePair("client_id", configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID)),
                new NameValuePair("client_secret", configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET))
        };

        try {
            httpClient.executeMethod(post);
            String body = post.getResponseBodyAsString();
            return mapper.readValue(body, TokenResponse.class);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * 获取用户信息（返回 JSON 字符串）
     */
    public UserInfo getUserInfo(String accessToken) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT));

        get.setRequestHeader("Authorization", "Bearer " + accessToken);

        try {
            client.executeMethod(get);
            String body = get.getResponseBodyAsString();
            return mapper.readValue(body, UserInfo.class);
        } finally {
            get.releaseConnection();
        }
    }
}
