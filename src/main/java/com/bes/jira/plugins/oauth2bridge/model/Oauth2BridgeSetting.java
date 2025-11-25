package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Oauth2BridgeSetting {
    private List<ClientConfigPair> clientConfigPairs = new ArrayList<>();
    private boolean insecureSkipVerify;
    private String trustCaCert;

    public Oauth2BridgeSetting() {
    }

    public Oauth2BridgeSetting(List<ClientConfigPair> clientConfigPairs, boolean insecureSkipVerify, String trustCaCert) {
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
        return "Oauth2BridgeSetting{" +
                ", insecureSkipVerify=" + insecureSkipVerify +
                ", trustCaCert='" + trustCaCert + '\'' +
                '}';
    }
}
