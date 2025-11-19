package com.bes.jira.plugins.oauth2bridge.util;

public class Encrpt {
    // 加密显示一段文本
    public static String mask(String s) {
        if (s == null || s.length() <= 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
