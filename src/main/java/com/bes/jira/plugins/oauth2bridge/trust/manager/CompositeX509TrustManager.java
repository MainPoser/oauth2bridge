package com.bes.jira.plugins.oauth2bridge.trust.manager;

import com.bes.jira.plugins.oauth2bridge.socket.factory.CustomSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompositeX509TrustManager implements X509TrustManager {

    private static final Logger log = LoggerFactory.getLogger(CompositeX509TrustManager.class);

    private final X509TrustManager[] managers;

    public CompositeX509TrustManager(List<X509TrustManager> managers) {
        this.managers = managers.toArray(new X509TrustManager[0]);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // 对于客户端证书校验，只要有一个 TrustManager 通过，就算通过
        for (X509TrustManager manager : managers) {
            try {
                manager.checkClientTrusted(chain, authType);
                return; // 至少一个通过，验证成功
            } catch (CertificateException e) {
                // 忽略当前异常，尝试下一个
            }
        }
        // 如果所有管理器都失败，则抛出最后一个异常
        if (managers.length > 0) {
            managers[managers.length - 1].checkClientTrusted(chain, authType);
        } else {
            throw new CertificateException("No TrustManagers available for client trust checking.");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // 对于服务器证书校验 (这是我们连接 IDP 时关注的)，只要有一个 TrustManager 通过，就算通过
        for (X509TrustManager manager : managers) {
            try {
                manager.checkServerTrusted(chain, authType);
                return; // 至少一个通过，验证成功
            } catch (CertificateException e) {
                // 忽略当前异常，尝试下一个
            }
        }
        // 如果所有管理器都失败，则抛出最后一个异常
        if (managers.length > 0) {
            managers[managers.length - 1].checkServerTrusted(chain, authType);
        } else {
            throw new CertificateException("No TrustManagers available for server trust checking.");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // 返回所有 TrustManager 接受的颁发者列表的并集
        List<X509Certificate> issuers = new ArrayList<>();
        for (X509TrustManager manager : managers) {
            Collections.addAll(issuers, manager.getAcceptedIssuers());
        }
        return issuers.toArray(new X509Certificate[0]);
    }
}
