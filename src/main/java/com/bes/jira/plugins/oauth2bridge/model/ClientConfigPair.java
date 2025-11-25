package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientConfigPair {
    private String callback;
    private String clientId;

    public ClientConfigPair() {}

    public ClientConfigPair(String callback, String clientId) {
        this.callback = callback;
        this.clientId = clientId;
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
}
