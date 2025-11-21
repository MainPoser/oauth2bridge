package com.bes.jira.plugins.oauth2bridge.rest;

import com.bes.jira.plugins.oauth2bridge.cache.TokenCache;
import com.bes.jira.plugins.oauth2bridge.http.factory.HttpClientFactory;
import com.bes.jira.plugins.oauth2bridge.wrapper.CachingHttpServletRequest;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Path("/oauth")
@AnonymousAllowed
public class Oauth {
    private static final Logger log = LoggerFactory.getLogger(Oauth.class);
    private final HttpClientFactory httpClientFactory;
    private final SettingService settingService;
    private final TokenCache tokenCache;

    @Inject
    public Oauth(HttpClientFactory httpClientFactory, SettingService settingService, TokenCache tokenCache) {
        this.httpClientFactory = httpClientFactory;
        this.settingService = settingService;
        this.tokenCache = tokenCache;
    }

    @GET
    @Path("authorize")
    @Produces(MediaType.WILDCARD)
    public Response authorize(@Context HttpServletRequest request) {
        log.debug("Request authorize, redirect to remote oauth2");
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        URI uri = UriBuilder.fromUri(settingService.getSetting().getAuthorizeEndpoint() + queryString).build();
        return Response.seeOther(uri).build();
    }

    @Path("invoke")
    @POST
    public void invoke(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        log.debug("Request invoke, do local and proxy to remote oauth2");
        CachingHttpServletRequest wrappedRequest;
        try {
            // 1. **创建包装器**：在这一步，原始请求体被读取并缓存。
            wrappedRequest = new CachingHttpServletRequest(request);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Failed to cache request body: " + e.getMessage());
            } catch (Exception ignored) {
            }
            return;
        }
        // 2. **提取本地需要的 Token 字段（使用缓存的 Body）**
        String requestBodyString = wrappedRequest.getBodyString();
        Map<String, String> formParams = new HashMap<>();
        // 如果 Body 存在，解析表单参数
        if (!requestBodyString.isEmpty() && wrappedRequest.getContentType() != null && wrappedRequest.getContentType().startsWith("application/x-www-form-urlencoded")) {
            try {
                // 核心解析逻辑
                List<NameValuePair> params = URLEncodedUtils.parse(requestBodyString, StandardCharsets.UTF_8);

                // 将 List<NameValuePair> 转换为 Map<String, String>
                for (NameValuePair pair : params) {
                    // 默认使用 put()。如果 OAuth 请求中存在重复 key，这里会保留最后一个值。
                    formParams.put(pair.getName(), pair.getValue());
                }
            } catch (Exception e) {
                // 记录解析错误，仍然继续尝试 Basic Auth 或转发
                log.error("Error parsing form-urlencoded body: {}", e.getMessage());
            }
        }
        // 过期本地的指定token
        String queryAccessToken = request.getParameter("token");
        // 强制过期
        if (!queryAccessToken.isEmpty()) {
            tokenCache.invalidate(queryAccessToken);
        }
        if (!formParams.get("token").isEmpty()) {
            tokenCache.invalidate(formParams.get("token"));
        }
        forwardRequest(wrappedRequest, response, settingService.getSetting().getInvokeEndpoint());
    }

    // --- 代理 POST 请求 ---
    @POST
    @Path("{subPath: .*}") // 使用正则表达式捕获所有剩余路径
    @Produces(MediaType.WILDCARD)
    public void proxyPostRequests(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("subPath") String subPath) {
        forwardWrapperRequest(request, response, subPath);
    }

    public void forwardWrapperRequest(HttpServletRequest request, HttpServletResponse response, String subPath) {
        CachingHttpServletRequest wrappedRequest;
        try {
            // 1. **创建包装器**：在这一步，原始请求体被读取并缓存。
            wrappedRequest = new CachingHttpServletRequest(request);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Failed to cache request body: " + e.getMessage());
            } catch (Exception ignored) {
            }
            return;
        }
        forwardRequest(wrappedRequest, response, settingService.getSetting().getBaseEndpoint() + "/" + subPath);
    }

    /**
     * 将客户端请求代理到远程服务器，并将响应写回客户端
     */
    public void forwardRequest(HttpServletRequest request, HttpServletResponse response, String targetUrl) {
        CloseableHttpClient httpClient = httpClientFactory.createClient(settingService.getSetting().isInsecureSkipVerify(), settingService.getSetting().getTrustCaCert());
        try {
            // 创建请求对象
            HttpRequestBase proxyRequest;
            switch (request.getMethod().toUpperCase()) {
                case "POST":
                    proxyRequest = new HttpPost(targetUrl);
                    break;
                case "PUT":
                    proxyRequest = new HttpPut(targetUrl);
                    break;
                case "PATCH":
                    proxyRequest = new HttpPatch(targetUrl);
                    break;
                case "DELETE":
                    proxyRequest = new HttpDelete(targetUrl);
                    break;
                default:
                    proxyRequest = new HttpGet(targetUrl);
                    break;
            }

            // 设置请求体
            if (proxyRequest instanceof HttpEntityEnclosingRequestBase) {
                String contentType = request.getContentType();
                if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                    Map<String, String[]> paramMap = request.getParameterMap();
                    List<NameValuePair> formParams = new ArrayList<>();
                    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                        for (String value : entry.getValue()) {
                            formParams.add(new BasicNameValuePair(entry.getKey(), value));
                        }
                    }
                    ((HttpEntityEnclosingRequestBase) proxyRequest)
                            .setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
                } else if (request.getContentLength() > 0) {
                    InputStreamEntity entity = new InputStreamEntity(request.getInputStream(), request.getContentLength());
                    if (contentType != null) entity.setContentType(contentType);
                    ((HttpEntityEnclosingRequestBase) proxyRequest).setEntity(entity);
                }
            }

            // 复制请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!"host".equalsIgnoreCase(name) && !"content-length".equalsIgnoreCase(name)) {
                    proxyRequest.addHeader(name, request.getHeader(name));
                }
            }

            // 执行请求
            try (CloseableHttpResponse remoteResponse = httpClient.execute(proxyRequest)) {
                log.debug("Forward remoteResponse: {}", remoteResponse.getStatusLine());

                // 设置状态码
                response.setStatus(remoteResponse.getStatusLine().getStatusCode());

                // 复制响应头
                for (Header h : remoteResponse.getAllHeaders()) {
                    String name = h.getName();
                    if (!"transfer-encoding".equalsIgnoreCase(name) &&
                            !"content-length".equalsIgnoreCase(name) &&
                            !"set-cookie".equalsIgnoreCase(name) && // 仍然建议过滤，防止干扰
                            !"connection".equalsIgnoreCase(name)
                    ) { // <--- 必须过滤！交给 Tomcat 处理连接状态
                        log.debug("response header {}={}", name, h.getValue());
                        response.setHeader(h.getName(), h.getValue());
                    }
                }

                // 写入响应体
                HttpEntity entity = remoteResponse.getEntity();
                if (entity != null) {
                    try (
                            InputStream in = entity.getContent();
                            OutputStream out = response.getOutputStream()
                    ) {
                        IOUtils.copy(in, out);
                        out.flush();
                    }
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Proxy error: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }

}
