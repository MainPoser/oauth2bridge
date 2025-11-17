package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.model.TokenResponse;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Named;
import javax.ws.rs.core.MultivaluedMap;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public String buildAuthorizationUrl(MultivaluedMap<String, String> paramsForIdP) {
        // 1. 获取授权端点基地址
        String baseAuthorizationUrl = configService.getConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT);
        if (baseAuthorizationUrl == null || baseAuthorizationUrl.isEmpty()) {
            throw new IllegalStateException("OAuth 2.0 Authorization Endpoint is not configured.");
        }

        // 2. 确保包含 client_id (如果原始请求中没有)
        if (!paramsForIdP.containsKey("client_id")) {
            String clientId = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID);
            if (clientId != null) {
                paramsForIdP.put("client_id", Collections.singletonList(clientId));
            }
        }

        // 3. 构建查询字符串
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, java.util.List<String>> entry : paramsForIdP.entrySet()) {
            String key = entry.getKey();

            // ⚠️ 安全检查：授权请求中绝不应包含 client_secret
            if (key.equalsIgnoreCase("client_secret")) {
                continue;
            }

            for (String value : entry.getValue()) {
                if (query.length() > 0) {
                    query.append("&");
                }

                try {
                    // 编码键和值
                    query.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()))
                            .append("=")
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    // 抛出 RuntimeException，因为 UTF-8 编码失败是致命错误
                    throw new RuntimeException("Error encoding URL parameters for IdP.", e);
                }
            }
        }

        // 4. 拼接基地址和查询字符串
        String separator = baseAuthorizationUrl.contains("?") ? "&" : "?";

        return baseAuthorizationUrl + separator + query;
    }

    /**
     * 使用 code 换 token（返回 JSON 字符串）
     */
    public TokenResponse requestToken(MultivaluedMap<String, String> paramsForIdP) throws IOException {
        HttpClient httpClient = new HttpClient();
        PostMethod post = new PostMethod(configService.getConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT));
        List<NameValuePair> nameValuePairs = new ArrayList<>();

        // A. 遍历 paramsForIdP 中的所有参数
        for (Map.Entry<String, List<String>> entry : paramsForIdP.entrySet()) {
            String key = entry.getKey();
            // MultivaluedMap 的值是 List，理论上在 OAuth token 交换中应为单值
            for (String value : entry.getValue()) {
                nameValuePairs.add(new NameValuePair(key, value));
            }
        }
        post.setRequestBody(nameValuePairs.toArray(new NameValuePair[0]));

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
            int status = client.executeMethod(get);
            // ---- token 是否失效！ ----
            if (status == 401 || status == 403) {
                throw new InvalidParameterException("TOKEN_INVALID");
            }
            if (status != 200) {
                throw new IOException("Userinfo returned HTTP status " + status);
            }
            String body = get.getResponseBodyAsString();
            return mapper.readValue(body, UserInfo.class);
        } finally {
            get.releaseConnection();
        }
    }
}
