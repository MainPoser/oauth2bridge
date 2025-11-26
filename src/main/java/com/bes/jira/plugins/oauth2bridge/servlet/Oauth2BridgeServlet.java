package com.bes.jira.plugins.oauth2bridge.servlet;

import com.atlassian.jira.util.UrlBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.model.CallbackResponse;
import com.bes.jira.plugins.oauth2bridge.model.ClientConfigPair;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.RedirectException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Named
public class Oauth2BridgeServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final UserManager userManager;
    private final HttpClientFactory httpClientFactory;
    private final SettingService settingService;

    @Inject
    public Oauth2BridgeServlet(@ComponentImport UserManager userManager, HttpClientFactory httpClientFactory, SettingService settingService) {
        this.httpClientFactory = httpClientFactory;
        this.settingService = settingService;
        this.userManager = userManager;
    }

    /**
     * 将 REST @GET 方法的逻辑迁移到 Servlet 的 doGet 方法中。
     * http://localhost:2990/jira/plugins/servlet/oauth2bridge?client_id=ty&callback=http%3a%2f%2f192.168.0.130%3a8080%2fusermanager%2fcheck_token
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String clientId = req.getParameter("client_id");
        String callback = req.getParameter("callback");

        log.debug("[OAuth2Bridge] Received params: client_id='{}', callback='{}'", clientId, callback);

        resp.setContentType(ContentType.APPLICATION_JSON.toString());

        // 参数校验
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(callback)) {
            log.warn("[OAuth2Bridge] Missing required param. client_id='{}', callback='{}'", clientId, callback);
            resp.setStatus(HttpStatus.SC_BAD_REQUEST);
            resp.getWriter().write("client_id or callback is blank");
            return;
        }

        // 解析 callback URL
        URL url = UrlBuilder.createURL(callback);
        String matchCallbackUrl = url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + url.getPath();

        log.info("[OAuth2Bridge] Normalized callback URL: {}", matchCallbackUrl);

        // 检查 clientId + callback 是否被允许
        ClientConfigPair allowClientConfigPair = null;
        for (ClientConfigPair config : settingService.getSetting().getClientConfigPairs()) {
            log.debug("[OAuth2Bridge] Checking allowed pair: clientId={}, callback={}", config.getClientId(), config.getCallback());
            if (config.getClientId().equals(clientId) && config.getCallback().equals(matchCallbackUrl)) {
                allowClientConfigPair = config;
                break;
            }
        }

        if (allowClientConfigPair == null) {
            log.warn("[OAuth2Bridge] Reject request. No matching client config. clientId='{}', callback='{}'", clientId, matchCallbackUrl);
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().write(MessageFormatter.format("client_id:{} callback:{} is not allowed", clientId, callback).getMessage());
            return;
        }

        // 用户校验
        UserProfile remoteUser = userManager.getRemoteUser(req);
        if (remoteUser == null) {
            String requestUrl = req.getRequestURL().toString();
            String queryString = req.getQueryString();

            if (queryString != null) {
                requestUrl += "?" + queryString;
            }
            log.info("[OAuth2Bridge] User not logged in. Redirecting to login page. os_destination={}", requestUrl);
            String redirectUrl = req.getContextPath() + "/login.jsp?os_destination=" + URLEncoder.encode(requestUrl, StandardCharsets.UTF_8.toString());
            resp.sendRedirect(redirectUrl);
            return;
        }

        log.info("[OAuth2Bridge] Authenticated user: {}", remoteUser.getUsername());

        // 提取 Cookies（不打印敏感）
        ObjectNode cookiesAsJson = extractCookiesAsJson(req);
        log.debug("[OAuth2Bridge] Extracted cookies JSON: {}", cookiesAsJson);

        // 内部回调 POST
        StringEntity entity = new StringEntity(cookiesAsJson.toString(), ContentType.APPLICATION_JSON);
        HttpPost post = new HttpPost(callback);

        log.info("[OAuth2Bridge] Forwarding cookies to callback via POST. callback={}", callback);

        post.setEntity(entity);


        CloseableHttpClient httpClient = httpClientFactory.createClient(settingService.getSetting().isInsecureSkipVerify(), settingService.getSetting().getTrustCaCert());

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            log.info("[OAuth2Bridge] Callback response status: {}", status);

            String redirectUri = allowClientConfigPair.getRedirectUrl();

            if (status == HttpStatus.SC_OK) {
                if (response.getEntity() != null) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.debug("[OAuth2Bridge] Callback response body: {}", responseBody);

                    try {
                        CallbackResponse callbackResponse = objectMapper.readValue(responseBody, CallbackResponse.class);

                        if (StringUtils.isNotBlank(callbackResponse.getRedirectUrl())) {
                            redirectUri = callbackResponse.getRedirectUrl();
                            log.info("[OAuth2Bridge] Redirect URI overridden by callback: {}", redirectUri);
                        }
                    } catch (Exception e) {
                        log.warn("[OAuth2Bridge] Failed to parse callback response JSON. Using default redirectUrl. body={}", responseBody);
                    }
                }

                log.info("[OAuth2Bridge] Redirecting user to final redirectUri={}", redirectUri);
                if (StringUtils.isBlank(redirectUri)) {

                    log.warn("[OAuth2Bridge] redirectUri is empty. Using Jira system error page.");

                    String message = "The OAuth2 callback did not return redirectUri for client_id="
                            + clientId + ". and not config redirectUrl";
                    throw new RedirectException(message);
                } else {
                    resp.sendRedirect(redirectUri);
                }
            } else {
                String errorBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "<empty>";
                throw new HttpException(MessageFormatter.format("[OAuth2Bridge] Callback returned error. status={}, body={}", status, errorBody).getMessage());
            }
        } catch (IOException | HttpException e) {
            log.error("[OAuth2Bridge] Callback request failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.info("[OAuth2Bridge] Completed. cost={}ms", (System.currentTimeMillis() - start));
    }

    /**
     * 将 REST 的辅助方法 extractCookiesAsJson 迁移到 Servlet 中
     */
    private ObjectNode extractCookiesAsJson(HttpServletRequest request) {
        ObjectNode json = objectMapper.createObjectNode();

        Cookie[] cookies = request.getCookies();
        String cookieStr = (cookies == null) ? "" : Arrays.stream(cookies).map(c -> c.getName() + "=" + c.getValue()).reduce((c1, c2) -> c1 + "; " + c2).orElse("");

        log.debug("Combined cookie string: {}", cookieStr);
        json.put("cookie", cookieStr);

        return json;
    }
}