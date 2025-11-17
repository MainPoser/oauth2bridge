package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonProperty;

public class TokenResponse {

    /**
     * Access token 字段，对应 JSON: access_token
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Token 类型，对应 JSON: token_type
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Refresh token，对应 JSON: refresh_token
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 过期时间（秒），对应 JSON: expires_in
     */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * scope 字段（可选），对应 JSON: scope
     */
    @JsonProperty("scope")
    private String scope;

    // ----------- Getter / Setter -------------
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "access_token='" + accessToken + '\'' +
                ", token_type='" + tokenType + '\'' +
                ", refresh_token='" + refreshToken + '\'' +
                ", expires_in=" + expiresIn +
                ", scope='" + scope + '\'' +
                '}';
    }
}