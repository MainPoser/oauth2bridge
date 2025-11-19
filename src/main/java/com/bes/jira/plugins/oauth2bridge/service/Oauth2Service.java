package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
        String requestUrl = postMethod.getURI().toString();
        log.debug(">> [Introspection] Sending POST request to: {}", requestUrl);
        try {
            int status = client.executeMethod(postMethod);
            String responseBody = null;

            // 始终尝试读取响应体，以便在错误时获取详细的错误信息
            try (InputStream responseStream = postMethod.getResponseBodyAsStream()) {
                if (responseStream != null) {
                    responseBody = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                // 记录读取响应体时的IO错误，但不中断主流程
                log.warn("!! [Introspection] Failed to read response body for status {}: {}", status, e.getMessage());
            }

            // --- 失败路径：401 或 403 (认证失败) ---
            if (status == 401 || status == 403) {
                log.warn("!! [Introspection] Authorization server rejected token. Status: {}. Response body: {}", status, responseBody);
                throw new InvalidParameterException("TOKEN_INVALID");
            }

            // --- 失败路径：非 200 且非 401/403 的其他 HTTP 错误 ---
            if (status != 200) {
                log.error("!! [Introspection] Request failed. Returned HTTP status: {}. Response body: {}", status, responseBody);
                throw new IOException("Introspection returned HTTP status " + status);
            }

            // --- 成功路径：200 OK ---
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("!! [Introspection] Successful status 200, but response body is empty!");
                throw new IOException("Introspection succeeded (200), but response body was empty.");
            }

            // 成功时打印响应体摘要
            log.debug("<< [Introspection] SUCCESS 200. Response body summary: {}", responseBody);

            // 反序列化
            return mapper.readValue(responseBody, Introspection.class);
        }catch (IOException e) {
            // 捕获 HTTP 客户端执行错误或 JSON/IO 错误
            log.error("!! [Introspection] Fatal I/O or JSON parsing error during request to {}: {}", requestUrl, e.getMessage(), e);
            throw e; // 重新抛出，让上层调用者处理
        } finally {
            log.debug("<< [Introspection] Releasing connection for request to: {}", requestUrl);
            postMethod.releaseConnection();
        }
    }
}
