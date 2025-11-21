package com.bes.jira.plugins.oauth2bridge.service;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.Introspection;
import com.bes.jira.plugins.oauth2bridge.model.IntrospectionResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Base64;
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
    public IntrospectionResponse introspection(String accessToken) throws IOException, URISyntaxException {
        CloseableHttpClient client = httpClientFactory.createClient(
                settingService.getSetting().isInsecureSkipVerify(),
                settingService.getSetting().getTrustCaCert()
        );
        // 使用目标 URL 创建 Builder
        URIBuilder uriBuilder = new URIBuilder(settingService.getSetting().getIntrospectionEndpoint());
        // 添加参数 (会自动处理 URL 编码)
        uriBuilder.addParameter("token", accessToken);
        URI build = uriBuilder.build();
        HttpPost httpPost = new HttpPost(build);
        // 拼接 "username:password"
        String auth = settingService.getSetting().getClientId() + ":" + settingService.getSetting().getClientSecret();
        // Base64 编码
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        // 设置 Authorization 头
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        log.debug(">> [Introspection] Sending POST request to: {}", build.toString());
        try (CloseableHttpResponse response = client.execute(httpPost)) {
            int httpResponseStatus = response.getStatusLine().getStatusCode();

            // 2. 安全读取响应体 (替代原来的 IOUtils 逻辑)
            String responseBody = null;
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try {
                    // EntityUtils.toString 会自动读取流并关闭流
                    responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    // 保持你原来的逻辑：记录错误但不中断，以便后续根据状态码处理
                    log.warn("!! [Introspection] Failed to read response body for status {}: {}", httpResponseStatus, e.getMessage());
                }
            }

            // --- 失败路径：401 或 403 (认证失败) ---
            if (httpResponseStatus == 401 || httpResponseStatus == 403) {
                log.warn("!! [Introspection] Authorization server rejected token. Status: {}. Response body: {}", httpResponseStatus, responseBody);
                return new IntrospectionResponse(null, httpResponseStatus, "TOKEN_INVALID");
            }

            // --- 失败路径：非 200 且非 401/403 的其他 HTTP 错误 ---
            if (httpResponseStatus != 200) {
                log.error("!! [Introspection] Request failed. Returned HTTP status: {}. Response body: {}", httpResponseStatus, responseBody);
                return new IntrospectionResponse(null, httpResponseStatus, responseBody);
            }

            // --- 成功路径：200 OK ---
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("!! [Introspection] Successful status 200, but response body is empty!");
                return new IntrospectionResponse(null, httpResponseStatus, "Response body empty");
            }

            // 成功时打印响应体摘要
            log.debug("<< [Introspection] SUCCESS 200. Response body summary: {}", responseBody);
            Introspection introspection = mapper.readValue(responseBody, Introspection.class);
            return new IntrospectionResponse(introspection, httpResponseStatus, null);
        } catch (IOException e) {
            // 捕获 HTTP 客户端执行错误或 JSON/IO 错误
            log.error("!! [Introspection] Fatal I/O or JSON parsing error during request: {}", e.getMessage(), e);
            throw e; // 重新抛出，让上层调用者处理
        }
    }
}
