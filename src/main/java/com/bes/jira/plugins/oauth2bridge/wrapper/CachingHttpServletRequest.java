package com.bes.jira.plugins.oauth2bridge.wrapper;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class CachingHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachingHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        // 1. 读取原始请求的输入流，并缓存为字节数组
        try (InputStream is = request.getInputStream();) {
            this.cachedBody = IOUtils.toByteArray(is);
        }
    }

    /**
     * 重写 getInputStream()：返回一个基于缓存字节数组的新输入流。
     * 这样可以多次读取。
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(this.cachedBody);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }

    /**
     * 重写 getReader()：返回一个基于缓存字节数组的 Reader。
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding() == null ? StandardCharsets.UTF_8.name() : getCharacterEncoding()));
    }

    /**
     * 辅助方法：获取缓存的请求体字符串，便于参数解析。
     */
    public String getBodyString() {
        return new String(this.cachedBody, StandardCharsets.UTF_8);
    }
}