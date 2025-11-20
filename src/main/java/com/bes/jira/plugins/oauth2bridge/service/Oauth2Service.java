package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

@Named
public class Oauth2Service {

    private static final Logger log = LoggerFactory.getLogger(Oauth2Service.class);

    private final SettingService settingService;
    private final HttpClientFactory httpClientFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    public Oauth2Service(SettingService settingService, HttpClientFactory httpClientFactory) {
        this.settingService = settingService;
        this.httpClientFactory = httpClientFactory;
    }

    /**
     * 校验token是否有效并获取用户信息
     */
    public Introspection introspection(String accessToken) throws IOException {
        CloseableHttpClient client = httpClientFactory.createClient(
                settingService.getSetting().isInsecureSkipVerify(),
                settingService.getSetting().getTrustCaCert()
        );
        // 1. 创建 HttpPost 对象
        HttpPost httpPost = new HttpPost(settingService.getSetting().getIntrospectionEndpoint());

        // 2. 准备表单参数 (注意：这里使用 List 替代了 3.x 的数组)
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", settingService.getSetting().getClientId()));
        params.add(new BasicNameValuePair("client_secret", settingService.getSetting().getClientSecret()));
        params.add(new BasicNameValuePair("token", accessToken));

        // 3. 将参数封装为 Entity 并设置编码
        // UrlEncodedFormEntity 会自动将 Content-Type 设置为 application/x-www-form-urlencoded
        httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        String requestUrl = httpPost.getURI().toString();
        log.debug(">> [Introspection] Sending POST request to: {}", requestUrl);
        try (CloseableHttpResponse response = client.execute(httpPost)) {
            int httpResponse = response.getStatusLine().getStatusCode();

            // 2. 安全读取响应体 (替代原来的 IOUtils 逻辑)
            String responseBody = null;
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try {
                    // EntityUtils.toString 会自动读取流并关闭流
                    responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    // 保持你原来的逻辑：记录错误但不中断，以便后续根据状态码处理
                    log.warn("!! [Introspection] Failed to read response body for status {}: {}", httpResponse, e.getMessage());
                }
            }

            // --- 失败路径：401 或 403 (认证失败) ---
            if (httpResponse == 401 || httpResponse == 403) {
                log.warn("!! [Introspection] Authorization server rejected token. Status: {}. Response body: {}", httpResponse, responseBody);
                throw new InvalidParameterException("TOKEN_INVALID");
            }

            // --- 失败路径：非 200 且非 401/403 的其他 HTTP 错误 ---
            if (httpResponse != 200) {
                log.error("!! [Introspection] Request failed. Returned HTTP status: {}. Response body: {}", httpResponse, responseBody);
                throw new IOException("Introspection returned HTTP status " + httpResponse);
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
        } catch (IOException e) {
            // 捕获 HTTP 客户端执行错误或 JSON/IO 错误
            log.error("!! [Introspection] Fatal I/O or JSON parsing error during request to {}: {}", requestUrl, e.getMessage(), e);
            throw e; // 重新抛出，让上层调用者处理
        } finally {
            log.debug("<< [Introspection] Releasing connection for request to: {}", requestUrl);
        }
    }
}
