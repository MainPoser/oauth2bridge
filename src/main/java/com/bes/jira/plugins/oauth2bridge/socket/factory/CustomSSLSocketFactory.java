package com.bes.jira.plugins.oauth2bridge.socket.factory;

import com.bes.jira.plugins.oauth2bridge.trust.manager.CompositeX509TrustManager;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CustomSSLSocketFactory implements ProtocolSocketFactory {

    private static final Logger log = LoggerFactory.getLogger(CustomSSLSocketFactory.class);

    private final boolean insecureSkipVerify;
    private final String trustCaCert;

    private SSLContext sslContext = null;

    public CustomSSLSocketFactory(boolean insecureSkipVerify, String trustCaCert) {
        this.insecureSkipVerify = insecureSkipVerify;
        this.trustCaCert = trustCaCert;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return getSSLContext().getSocketFactory().createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    private SSLContext getSSLContext() {
        if (this.sslContext == null) {
            this.sslContext = createSSLContext();
        }
        return this.sslContext;
    }

    // ----------------------------------------------------
    // SSLContext 初始化逻辑 (核心)
    // ----------------------------------------------------
    private SSLContext createSSLContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers;

            if (insecureSkipVerify) {
                // 模式 1: 不安全模式 (信任所有证书)
                trustManagers = new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};
            } else if (trustCaCert != null && !trustCaCert.isEmpty()) {
                // 模式 2: 自定义 CA 模式
                trustManagers = createCustomCATrustManagers();
            } else {
                // 模式 3: 系统默认模式
                trustManagers = getDefaultTrustManagers();
            }
            context.init(null, trustManagers, new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSL context.", e);
        }
    }

    // 获取 JRE 默认的 TrustManagers
    private TrustManager[] getDefaultTrustManagers() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // 初始化为 JRE 默认的信任库
        return tmf.getTrustManagers();
    }

    // 创建包含自定义 CA 的 TrustManagers
    private TrustManager[] createCustomCATrustManagers() throws Exception {
        // -------------------------------------------------------------------
        // 1. 获取默认 Trust Managers (信任 JRE 默认的 CA)
        // -------------------------------------------------------------------
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null); // null 表示使用 JRE 默认的 KeyStore

        List<X509TrustManager> trustManagers = new ArrayList<>();

        for (TrustManager tm : defaultTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManagers.add((X509TrustManager) tm);
            }
        }

        // -------------------------------------------------------------------
        // 2. 获取自定义 Trust Managers (信任传入的自签名 CA)
        // -------------------------------------------------------------------
        KeyStore customKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        customKeyStore.load(null, null);

        // 解析 PEM 证书（此处的 trustKeystore 应该是 customCaPem 字段名）
        String base64Cert = trustCaCert // 使用类成员 customCaPem
                .replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");

        // 解析证书和添加到 KeyStore
        byte[] decodedCert = Base64.getDecoder().decode(base64Cert);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decodedCert));
        customKeyStore.setCertificateEntry("custom-ca", caCert);

        // 初始化 TrustManagerFactory 使用自定义 KeyStore
        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(customKeyStore);

        // 将自定义的 X509TrustManager 添加到列表中
        for (TrustManager tm : customTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManagers.add((X509TrustManager) tm);
            }
        }

        // -------------------------------------------------------------------
        // 3. 创建复合 Trust Manager 并返回
        // -------------------------------------------------------------------
        // 将所有 TrustManager 包装成一个 CompositeX509TrustManager
        CompositeX509TrustManager compositeManager = new CompositeX509TrustManager(trustManagers);

        // SSLContext 只需要一个 X509TrustManager
        return new TrustManager[]{compositeManager};
    }
}
