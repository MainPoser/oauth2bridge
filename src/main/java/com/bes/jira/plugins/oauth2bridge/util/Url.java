package com.bes.jira.plugins.oauth2bridge.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Url {
    public static String buildQueryUrl(String baseUrl, Map<String, String> queryParams) {
        // 1. 创建 StringBuilder 用于构建查询字符串
        StringBuilder query = new StringBuilder();

        // 2. 遍历参数，并对值进行编码
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (query.length() != 0) {
                query.append("&"); // 在第二个及以后的参数前添加 &
            }

            String encodedValue;
            try {
                // 必须对值进行编码
                encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                // 编码失败处理
                throw new RuntimeException("URL 编码失败", e);
            }

            // 3. 拼接键和已编码的值
            query.append(entry.getKey()).append("=").append(encodedValue);
        }

        // 4. 返回最终 URL
        return baseUrl + "?" + query.toString();
    }
}
