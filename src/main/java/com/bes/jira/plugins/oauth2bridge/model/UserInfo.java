package com.bes.jira.plugins.oauth2bridge.model;

import org.codehaus.jackson.annotate.JsonProperty;

public class UserInfo {

    /**
     * 用户唯一 ID，对应 JSON: sub
     */
    @JsonProperty("sub")
    private String sub;

    /**
     * 全名，对应 JSON: name
     */
    @JsonProperty("name")
    private String name;

    /**
     * 名，对应 JSON: given_name
     */
    @JsonProperty("given_name")
    private String givenName;

    /**
     * 姓，对应 JSON: family_name
     */
    @JsonProperty("family_name")
    private String familyName;

    /**
     * 邮箱，对应 JSON: email
     */
    @JsonProperty("email")
    private String email;

    /**
     * 用户头像，对应 JSON: picture
     */
    @JsonProperty("picture")
    private String picture;

    // ----------- Getter / Setter -------------
    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "sub='" + sub + '\'' +
                ", name='" + name + '\'' +
                ", given_name='" + givenName + '\'' +
                ", family_name='" + familyName + '\'' +
                ", email='" + email + '\'' +
                ", picture='" + picture + '\'' +
                '}';
    }
}
