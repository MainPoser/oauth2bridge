package com.bes.jira.plugins.authbridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientConfigPair {
    private String callback;
    private String clientId;
    private String redirectUrl;

    public ClientConfigPair() {}

    public ClientConfigPair(String callback, String clientId, String redirectUrl) {
        this.callback = callback;
        this.clientId = clientId;
        this.redirectUrl = redirectUrl;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
