package com.bes.jira.plugins.oauth2bridge.util;

import java.security.SecureRandom;
import java.util.Base64;

public class OAuthStateGenerator {
    // 推荐的长度：32字节 (~43个 Base64 字符)
    private static final int STATE_LENGTH_BYTES = 32;

    /**
     * 生成一个 URL 安全、不可预测的 OAuth state 字符串。
     * @return 随机生成的 Base64 URL Safe 字符串。
     */
    public static String generateState() {
        // 1. 获取一个密码学安全的随机数生成器
        SecureRandom secureRandom = new SecureRandom();

        // 2. 生成随机字节数组
        byte[] randomBytes = new byte[STATE_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);

        // 3. 使用 Base64 URL Safe 编码
        // Base64.getUrlEncoder() 确保编码后的字符串是 URL 友好的，
        // 避免了在 URL 中需要编码 '+'、'/' 和 '=' 字符的问题。
        return Base64.getUrlEncoder()
                .withoutPadding() // 移除末尾的等号填充，使 URL 更干净
                .encodeToString(randomBytes);
    }
}
