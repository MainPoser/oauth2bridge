package com.bes.jira.plugins.authbridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthBridgeSetting {
    private List<ClientConfigPair> clientConfigPairs = new ArrayList<>();
    private boolean insecureSkipVerify;
    private String trustCaCert;

    public AuthBridgeSetting() {
    }

    public AuthBridgeSetting(List<ClientConfigPair> clientConfigPairs, boolean insecureSkipVerify, String trustCaCert) {
        this.clientConfigPairs = clientConfigPairs;
        this.insecureSkipVerify = insecureSkipVerify;
        this.trustCaCert = trustCaCert;
    }

    public List<ClientConfigPair> getClientConfigPairs() {
        return clientConfigPairs;
    }

    public void setClientConfigPairs(List<ClientConfigPair> clientConfigPairs) {
        this.clientConfigPairs = clientConfigPairs;
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

    @Override
    public String toString() {
        return "AuthBridgeSetting{" +
                ", insecureSkipVerify=" + insecureSkipVerify +
                ", trustCaCert='" + trustCaCert + '\'' +
                '}';
    }
}
