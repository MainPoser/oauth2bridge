package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Introspection {

    /**
     * 是否启用
     */
    @JsonProperty("active")
    private boolean active;

    /**
     * 用户唯一 ID，对应 JSON: sub
     */
    @JsonProperty("sub")
    private String sub;

    /**
     * 全名，对应 JSON: user_name
     */
    @JsonProperty("user_name")
    private String username;

    /**
     * 认证时间，对应 JSON: auth_time
     */
    @JsonProperty("auth_time")
    private long authTime;

    /**
     * 过期时间，对应 JSON: exp
     */
    @JsonProperty("exp")
    private long exp;

    // ----------- Getter / Setter -------------

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public long getAuthTime() {
        return authTime;
    }

    public void setAuthTime(long authTime) {
        this.authTime = authTime;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }
}
