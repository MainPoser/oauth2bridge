package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeSetting;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Named
public class Oauth2BridgeAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeAction.class);

    private final SettingService settingService;

    private String introspectionEndpoint;
    private String baseEndpoint;
    private String invokeEndpoint;
    private String authorizeEndpoint;
    private String clientId;
    private String clientSecret;
    public boolean insecureSkipVerify;
    public String trustCaCert;
    public long sessionTimeoutSec;

    @Inject
    public Oauth2BridgeAction(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public String doDefault() {
        Oauth2BridgeSetting setting = settingService.getSetting();
        introspectionEndpoint = setting.getIntrospectionEndpoint();
        clientId = setting.getClientId();
        clientSecret = setting.getClientSecret();
        insecureSkipVerify = setting.isInsecureSkipVerify();
        trustCaCert = setting.getTrustCaCert();
        sessionTimeoutSec = setting.getSessionTimeoutSec();
        log.debug("Loaded setting: {}", setting);
        return INPUT;
    }

    @Override
    public String doExecute() throws IOException {
        if (this.command == null || this.command.isEmpty()) {
            return this.doDefault();
        }
        if (!baseEndpoint.isEmpty()) {
            if (introspectionEndpoint.isEmpty()) {
                introspectionEndpoint = baseEndpoint + "/introspection";
            }
            if (invokeEndpoint.isEmpty()) {
                invokeEndpoint = baseEndpoint + "/invoke";
            }
            if (authorizeEndpoint.isEmpty()) {
                authorizeEndpoint = baseEndpoint + "/authorize";
            }
        }
        Oauth2BridgeSetting oauth2BridgeSetting = new Oauth2BridgeSetting(
                introspectionEndpoint, baseEndpoint, invokeEndpoint, authorizeEndpoint,
                clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec
        );
        log.debug("Saving setting: {}", oauth2BridgeSetting);
        settingService.updateSetting(oauth2BridgeSetting);
        return SUCCESS;
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
}