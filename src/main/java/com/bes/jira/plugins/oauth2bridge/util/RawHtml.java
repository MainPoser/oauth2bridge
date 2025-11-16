package com.bes.jira.plugins.oauth2bridge.util;

import com.atlassian.velocity.htmlsafe.HtmlSafe;

public class RawHtml {

    private final String html;

    public RawHtml(String html) {
        this.html = html;
    }

    @Override
    @HtmlSafe  // ⭐⭐ 关键：告诉 Velocity 这是 “安全 HTML”，不要 escape
    public String toString() {
        return html;
    }
}
