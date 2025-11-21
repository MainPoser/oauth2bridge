package com.bes.jira.plugins.oauth2bridge.model;

public class IntrospectionResponse {
    private final Introspection introspection;
    private final int statusCode;
    private final String errorMessage;

    public IntrospectionResponse(Introspection introspection, int statusCode, String errorMessage) {
        this.introspection = introspection;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    public Introspection getIntrospection() {
        return introspection;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return introspection != null && statusCode == 200;
    }
}
