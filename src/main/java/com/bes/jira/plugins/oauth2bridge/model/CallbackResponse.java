package com.bes.jira.plugins.oauth2bridge.model;

public class CallbackResponse {
    private String redirectUri;

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
