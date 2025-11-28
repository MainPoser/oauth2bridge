package com.bes.jira.plugins.authbridge.http.factory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

@Named
public class HttpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    // --- 分别缓存三种类型的 Client ---
    private volatile CloseableHttpClient standardClient;
    private volatile CloseableHttpClient insecureClient;

    // 自定义证书 Client 及其对应的证书指纹（或内容字符串）
    private volatile CloseableHttpClient customClient;
    private String cachedCustomCertContent;

    private final Object lock = new Object();

    public HttpClientFactory() {
    }

    /**
     * 创建一个 HttpClient 实例，并为 Introspection Endpoint 局部设置自定义 SSL 协议。
     *
     * @return 局部配置好的 HttpClient 实例
     */
    public CloseableHttpClient createClient(boolean insecureSkipVerify, String trustCaCert) {
        // DEBUG: 记录请求策略，帮助开发排查配置优先级问题
        if (log.isDebugEnabled()) {
            log.debug("Requesting HttpClient. Strategy: InsecureSkipVerify={}, HasCustomCert={}",
                    insecureSkipVerify, StringUtils.isNotBlank(trustCaCert));
        }
        // 优先级 1: 如果要求跳过验证，返回 InsecureClient
        if (insecureSkipVerify) {
            return getInsecureClient();
        }

        // 优先级 2: 如果提供了证书，返回 CustomClient (会检查证书是否变化)
        if (!trustCaCert.isEmpty()) {
            return getCustomClient(trustCaCert);
        }

        // 优先级 3: 默认情况，返回 StandardClient
        return getStandardClient();
    }

    // ---------------------------------------------------------
    // 1. 获取 Standard Client (懒加载单例)
    // ---------------------------------------------------------
    private CloseableHttpClient getStandardClient() {
        if (standardClient != null) {
            return standardClient;
        }
        synchronized (lock) {
            if (standardClient == null) {
                log.info("Initializing Standard HttpClient (System Default SSL).");
                standardClient = createInternal(false, null);
            }
            return standardClient;
        }
    }

    // ---------------------------------------------------------
    // 2. 获取 Insecure Client (懒加载单例)
    // ---------------------------------------------------------
    private CloseableHttpClient getInsecureClient() {
        if (insecureClient != null) {
            return insecureClient;
        }
        synchronized (lock) {
            if (insecureClient == null) {
                log.info("Initializing Insecure HttpClient (Skip Verify enabled). Security warning: SSL validation is disabled.");
                insecureClient = createInternal(true, null);
            }
            return insecureClient;
        }
    }

    // ---------------------------------------------------------
    // 3. 获取 Custom Client (变更检测)
    // ---------------------------------------------------------
    private CloseableHttpClient getCustomClient(String trustCaCert) {
        // 检查是否已存在且证书内容未变
        if (customClient != null && Objects.equals(cachedCustomCertContent, trustCaCert)) {
            return customClient;
        }

        synchronized (lock) {
            // 双重检查
            if (customClient != null && Objects.equals(cachedCustomCertContent, trustCaCert)) {
                return customClient;
            }

            // 临时存储旧的客户端实例，以便在创建成功后关闭
            CloseableHttpClient oldClient = customClient;
            String logMessage;

            if (oldClient != null) {
                log.info("Configuration change detected: Custom Certificate content has changed. Re-initializing HttpClient.");
            } else {
                log.info("Initializing Custom HttpClient with provided Trusted Certificate.");
            }

            try {
                // 创建新的
                CloseableHttpClient newClient = createInternal(false, trustCaCert);
                // 创建成功后，才关闭旧的客户端
                if (oldClient != null) {
                    log.info("Successfully initialized new Custom HttpClient. Closing the old instance.");
                    closeClient(oldClient); // 关闭旧的
                }
                // 6. 更新全局变量为新的客户端实例
                customClient = newClient;
                // 7. 更新缓存的证书内容
                cachedCustomCertContent = trustCaCert;
                return customClient;
            } catch (Exception e) {
                log.error("Failed to initialize Custom HttpClient. The system will continue using the previous client if available.", e);
                // 保留抛出异常的逻辑，确保上层调用者知道客户端初始化失败
                throw new RuntimeException(e);
            }
        }
    }

    // ---------------------------------------------------------
    // 内部工厂方法 (复用创建逻辑)
    // ---------------------------------------------------------
    private CloseableHttpClient createInternal(boolean insecureSkipVerify, String trustCaCert) {
        try {
            SSLContext sslContext;
            SSLConnectionSocketFactory sslSocketFactory;

            if (insecureSkipVerify) {
                // Insecure 模式
                log.debug("Building SSLContext with Trust-All strategy.");
                sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            } else if (!trustCaCert.isEmpty()) {
                // Custom Cert 模式
                log.debug("Building SSLContext with Custom TrustStore.");
                KeyStore trustStore = createTrustStoreWithCert(trustCaCert);
                sslContext = SSLContexts.custom()
                        .loadTrustMaterial(trustStore, null)
                        .build();
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

            } else {
                // Standard 模式
                log.debug("Building SSLContext with System Default.");
                sslContext = SSLContexts.createSystemDefault();
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
            }

            return HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setMaxConnTotal(200)
                    .setMaxConnPerRoute(20)
                    .build();

        } catch (Exception e) {
            log.error("Error building SSLContext or HttpClient: {}", e.getMessage());
            throw new RuntimeException("Failed to create HttpClient", e);
        }
    }

    private KeyStore createTrustStoreWithCert(String certString) throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certInputStream = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(certInputStream);

            // DEBUG: 这里的关键是打印证书的主题（Subject），而不是打印证书内容。
            // 这样可以确认加载的是哪张证书，且不会泄露太多信息或刷屏。
            if (log.isDebugEnabled()) {
                log.debug("Loaded Custom Certificate. Subject DN: {}", certificate.getSubjectX500Principal());
            }

            keyStore.setCertificateEntry("custom-ca", certificate);
            return keyStore;
        } catch (Exception e) {
            log.error("Failed to parse the provided Custom Certificate string.", e);
            throw e;
        }
    }

    private void closeClient(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
                log.debug("HttpClient instance closed successfully.");
            } catch (IOException e) {
                log.warn("Error closing HttpClient resource.", e);
            }
        }
    }

    /**
     * 插件卸载时调用，清理所有资源
     */
    public void destroy() {
        log.info("Destroying HttpClientFactory. Closing all cached clients.");
        synchronized (lock) {
            closeClient(standardClient);
            closeClient(insecureClient);
            closeClient(customClient);
            standardClient = null;
            insecureClient = null;
            customClient = null;
        }
    }
}
