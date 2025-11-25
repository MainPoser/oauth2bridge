package com.bes.jira.plugins.oauth2bridge.servlet;

import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.ClientConfigPair;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Named
public class Oauth2BridgeServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClientFactory httpClientFactory;
    private final SettingService settingService;

    @Inject
    public Oauth2BridgeServlet(HttpClientFactory httpClientFactory, SettingService settingService) {
        this.httpClientFactory = httpClientFactory;
        this.settingService = settingService;
    }

    /**
     * 将 REST @GET 方法的逻辑迁移到 Servlet 的 doGet 方法中。
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Oauth2BridgeServlet doGet.");
        // 1. 获取参数 (使用 Servlet API)
        String clientId = req.getParameter("client_id");
        String callback = req.getParameter("callback");

        // 设置响应内容类型，防止返回 HTML 格式的错误
        resp.setContentType("application/json");

        // INFO: 记录关键的跳转行为
        log.info("Request session_get via Servlet.");

        // 2. 基础校验 (逻辑与原 REST 保持一致)
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(callback)) {
            resp.setStatus(HttpStatus.SC_BAD_REQUEST);
            resp.getWriter().write("client_id or callback is blank");
            return;
        }

        // 3. 校验是否匹配 (逻辑与原 REST 保持一致)
        // 注意：这里的校验逻辑似乎是反的，但在转换中我们保持原样
        boolean allow = false;
        for (ClientConfigPair clientConfigPair : settingService.getSetting().getClientConfigPairs()) {
            if (clientConfigPair.getClientId().equals(clientId) && clientConfigPair.getCallback().equals(callback)) {
                allow = true;
                break;
            }
        }

        if (!allow) {
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().write(MessageFormatter.format("client_id:{} callback: {} or callback is be not allowed", clientId, callback).getMessage());
            return;
        }

        // 4. 提取 Cookies
        ObjectNode cookiesAsJson = extractCookiesAsJson(req);

        // 5. 构造并发送内部 POST 请求
        StringEntity entity = new StringEntity(cookiesAsJson.toString(), StandardCharsets.UTF_8);
        HttpPost post = new HttpPost(callback);
        post.setEntity(entity);

        CloseableHttpClient httpClient = httpClientFactory.createClient(
                settingService.getSetting().isInsecureSkipVerify(),
                settingService.getSetting().getTrustCaCert()
        );

        // 6. 处理内部请求响应
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            // 将内部请求的状态码和实体转发给外部客户端
            resp.setStatus(response.getStatusLine().getStatusCode());

            // 确保响应内容被正确复制
            if (response.getEntity() != null) {
                String responseBody = EntityUtils.toString(response.getEntity());
                resp.getWriter().write(responseBody);
            }
        } catch (IOException e) {
            log.error("Request callback failed: {}", e.getMessage(), e); // 记录完整的错误信息
            resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Internal callback request failed\"}");
        }
    }

    /**
     * 将 REST 的辅助方法 extractCookiesAsJson 迁移到 Servlet 中
     */
    private ObjectNode extractCookiesAsJson(HttpServletRequest request) {
        ObjectNode json = objectMapper.createObjectNode();

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                // INFO: 确认成功捕获了所有 Cookie，包括 JSESSIONID
                log.debug("Captured cookie: {}", c.getName());
                json.put(c.getName(), c.getValue());
            }
        }
        return json;
    }
}