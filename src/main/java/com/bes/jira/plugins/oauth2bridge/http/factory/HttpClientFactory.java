package com.bes.jira.plugins.oauth2bridge.http.factory;

import com.bes.jira.plugins.oauth2bridge.socket.factory.CustomSSLSocketFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.net.URL;

@Named
public class HttpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    public HttpClientFactory() {
    }

    /**
     * 创建一个 HttpClient 实例，并为 Introspection Endpoint 局部设置自定义 SSL 协议。
     *
     * @return 局部配置好的 HttpClient 实例
     */
    public HttpClient createClient(boolean insecureSkipVerify, String trustCaCert, String introspectionEndpoint) {
        // 1. 创建 HttpClient 实例
        HttpClient httpClient = new HttpClient();

        // 2. 不跳过证书，且未配置自定义证书。返回默认客户端
        if (!insecureSkipVerify && "".equals(trustCaCert)) {
            return httpClient;
        }

        try {
            // 3. 解析 URL 以获取主机信息
            URL url = new URL(introspectionEndpoint);
            int port = url.getPort() == -1 ? 443 : url.getPort();

            // 4. 创建自定义 Socket Factory
            CustomSSLSocketFactory customFactory = new CustomSSLSocketFactory(insecureSkipVerify, trustCaCert);

            // 5. 创建自定义 Protocol 实例
            Protocol customHttps = new Protocol("https", customFactory, port);

            // 6. 【核心步骤】为这个 HttpClient 实例的目标主机局部设置 Protocol
            // 注意：HttpClient 3.x 没有针对所有主机的局部设置，必须针对目标主机设置
            httpClient.getHostConfiguration().setHost(url.getHost(), port, customHttps);
            return httpClient;
        } catch (Exception e) {
            // 记录异常，如果设置失败，则返回默认的 HttpClient
            log.error("CreateClient with CustomSSLSocketFactory Failed: {},use default client", e.getMessage());
            return new HttpClient();
        }
    }
}
