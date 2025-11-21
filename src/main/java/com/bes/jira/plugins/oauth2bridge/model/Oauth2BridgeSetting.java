package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Oauth2BridgeSetting {
    private String introspectionEndpoint;
    private String baseEndpoint;
    private String invokeEndpoint;
    private String authorizeEndpoint;
    private String clientId;
    private String clientSecret;
    private boolean insecureSkipVerify;
    private String trustCaCert;
    private long sessionTimeoutSec;

    public Oauth2BridgeSetting() {
    }

    public Oauth2BridgeSetting(String introspectionEndpoint, String baseEndpoint, String invokeEndpoint, String authorizeEndpoint, String clientId, String clientSecret, boolean insecureSkipVerify, String trustCaCert, long sessionTimeoutSec) {
        this.introspectionEndpoint = introspectionEndpoint;
        this.baseEndpoint = baseEndpoint;
        this.invokeEndpoint = invokeEndpoint;
        this.authorizeEndpoint = authorizeEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.insecureSkipVerify = insecureSkipVerify;
        this.trustCaCert = trustCaCert;
        this.sessionTimeoutSec = sessionTimeoutSec;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    public String getInvokeEndpoint() {
        return invokeEndpoint;
    }

    public void setInvokeEndpoint(String invokeEndpoint) {
        this.invokeEndpoint = invokeEndpoint;
    }

    public String getAuthorizeEndpoint() {
        return authorizeEndpoint;
    }

    public void setAuthorizeEndpoint(String authorizeEndpoint) {
        this.authorizeEndpoint = authorizeEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isInsecureSkipVerify() {
        return insecureSkipVerify;
    }

    public void setInsecureSkipVerify(boolean insecureSkipVerify) {
        this.insecureSkipVerify = insecureSkipVerify;
    }

    public String getTrustCaCert() {
        return trustCaCert;
    }

    public void setTrustCaCert(String trustCaCert) {
        this.trustCaCert = trustCaCert;
    }

    public long getSessionTimeoutSec() {
        return sessionTimeoutSec;
    }

    public void setSessionTimeoutSec(long sessionTimeoutSec) {
        this.sessionTimeoutSec = sessionTimeoutSec;
    }

    @Override
    public String toString() {
        return "Oauth2BridgeSetting{" +
                "introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", baseEndpoint='" + baseEndpoint + '\'' +
                ", invokeEndpoint='" + invokeEndpoint + '\'' +
                ", authorizeEndpoint='" + authorizeEndpoint + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", insecureSkipVerify=" + insecureSkipVerify +
                ", trustCaCert='" + trustCaCert + '\'' +
                ", sessionTimeoutSec=" + sessionTimeoutSec +
                '}';
    }
}
