package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.*;
import java.security.InvalidParameterException;

@Named
public class Oauth2Service {

    private static final Logger log = LoggerFactory.getLogger(Oauth2Service.class);

    private final Oauth2BridgeConfigService configService;
    private final HttpClientFactory httpClientFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    public Oauth2Service(Oauth2BridgeConfigService configService, HttpClientFactory httpClientFactory) {
        this.configService = configService;
        this.httpClientFactory = httpClientFactory;
    }

    /**
     * 校验token是否有效并获取用户信息
     */
    public Introspection introspection(String accessToken) throws IOException {
        HttpClient client = httpClientFactory.createClient();
        PostMethod postMethod = new PostMethod(configService.getConfig(Oauth2BridgeConfigService.KEY_INTROSPECTION_ENDPOINT));
        // 设置请求头（可选）
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        // 设置表单参数（核心）
        NameValuePair[] data = {
                new NameValuePair("client_id", configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID)),
                new NameValuePair("client_secret", configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET)),
                new NameValuePair("token", accessToken)
        };
        postMethod.setRequestBody(data);
        try {
            int status = client.executeMethod(postMethod);
            // ---- token 是否失效！ ----
            if (status == 401 || status == 403) {
                throw new InvalidParameterException("TOKEN_INVALID");
            }
            if (status != 200) {
                throw new IOException("Userinfo returned HTTP status " + status);
            }
            String body = postMethod.getResponseBodyAsString();
            return mapper.readValue(body, Introspection.class);
        } finally {
            postMethod.releaseConnection();
        }
    }
}
