package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.model.ClientConfigPair;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeSetting;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Named
public class Oauth2BridgeAction extends JiraWebActionSupport {


    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeAction.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SettingService settingService;

    public boolean insecureSkipVerify;
    public String trustCaCert;

    private List<ClientConfigPair> clientConfigPairs = Collections.emptyList();

    private String[] clientIds;
    private String[] callbacks;
    private String[] redirectUrls;

    @Inject
    public Oauth2BridgeAction(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public String doDefault() {
        log.info("User is accessing OAuth2 Bridge configuration page (doDefault).");

        Oauth2BridgeSetting setting = settingService.getSetting();
        if (setting != null) {
            insecureSkipVerify = setting.isInsecureSkipVerify();
            trustCaCert = setting.getTrustCaCert();
            clientConfigPairs = setting.getClientConfigPairs();

            log.info("Loaded existing settings: insecureSkipVerify={}, trustCaCert present={}, clientConfigPairs size={}",
                    insecureSkipVerify, trustCaCert != null && !trustCaCert.isEmpty(), clientConfigPairs.size());

            // 打印每一条 client 配置
            for (ClientConfigPair c : clientConfigPairs) {
                log.info("Loaded clientConfigPair - clientId: '{}', callback: '{}'", c.getClientId(), c.getCallback());
            }
        } else {
            log.info("No existing OAuth2 Bridge settings found, initializing defaults.");
        }

        return INPUT;
    }

    @Override
    public String doExecute() throws IOException {
        log.info("doExecute called with command='{}'", command);

        if (this.command == null || this.command.isEmpty()) {
            log.info("Command is empty, forwarding to doDefault.");
            return this.doDefault();
        }

        log.info("Starting OAuth2 Bridge settings update...");

        if (clientIds != null) {
            log.info("Received clientIds: {}", Arrays.toString(clientIds));
        } else {
            log.warn("Received clientIds is null!");
        }

        if (callbacks != null) {
            log.info("Received callbacks: {}", Arrays.toString(callbacks));
        } else {
            log.warn("Received callbacks is null!");
        }

        List<ClientConfigPair> newClientConfigPairs = new ArrayList<>();
        if (clientIds != null && callbacks != null && redirectUrls != null &&
                clientIds.length == callbacks.length && redirectUrls.length == callbacks.length) {
            for (int i = 0; i < clientIds.length; i++) {
                String clientId = clientIds[i];
                String callback = callbacks[i];
                String redirectUrl = redirectUrls[i];

                if (clientId != null && !clientId.trim().isEmpty() && callback != null && !callback.isEmpty()) {
                    newClientConfigPairs.add(new ClientConfigPair(callback, clientId.trim(), redirectUrl.trim()));
                    log.info("Adding clientConfigPair - clientId: '{}', callback: '{}', redirectUrl: '{}'", clientId.trim(), callback, redirectUrl);
                } else {
                    log.warn("Skipping empty or invalid clientConfigPair - clientId: '{}', callback: '{}', redirectUrl: '{}'", clientId, callback, redirectUrl);
                }
            }
        } else {
            log.warn("clientIds and callbacks arrays are null or have different lengths: clientIds.length={}, callbacks.length={}",
                    clientIds != null ? clientIds.length : "null",
                    callbacks != null ? callbacks.length : "null");
        }

        Oauth2BridgeSetting oauth2BridgeSetting = new Oauth2BridgeSetting(newClientConfigPairs, insecureSkipVerify, trustCaCert);
        log.info("Prepared Oauth2BridgeSetting for saving: clientConfigPairs size={}, insecureSkipVerify={}, trustCaCert present={}",
                newClientConfigPairs.size(), insecureSkipVerify, trustCaCert != null && !trustCaCert.isEmpty());

        try {
            settingService.updateSetting(oauth2BridgeSetting);
            log.info("OAuth2 Bridge settings updated successfully.");
        } catch (Exception e) {
            log.error("Failed to update OAuth2 Bridge settings.", e);
            addErrorMessage("Failed to save settings: " + e.getMessage());
            return ERROR;
        }

        return getRedirect("oauth2bridge.jspa");
    }

    // Getters / Setters
    public List<ClientConfigPair> getClientConfigPairs() {
        return clientConfigPairs;
    }

    public String[] getClientIds() {
        return clientIds;
    }

    public void setClientIds(String[] clientIds) {
        this.clientIds = clientIds;
    }

    public String[] getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(String[] callbacks) {
        this.callbacks = callbacks;
    }

    public String[] getRedirectUrls() {
        return redirectUrls;
    }

    public void setRedirectUrls(String[] redirectUrls) {
        this.redirectUrls = redirectUrls;
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
}