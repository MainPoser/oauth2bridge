package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.UserInfo;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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
     * 获取用户信息（返回 JSON 字符串）
     */
    public UserInfo getUserInfo(String accessToken) throws IOException {
        HttpClient client = httpClientFactory.createClient();
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
