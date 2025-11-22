package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeSetting;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.apache.commons.lang3.StringUtils;
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
// INFO: 记录用户进入页面的行为，便于审计
        log.info("User is accessing OAuth2 Bridge configuration page.");

        Oauth2BridgeSetting setting = settingService.getSetting();
        if (setting != null) {
            introspectionEndpoint = setting.getIntrospectionEndpoint();
            baseEndpoint = setting.getBaseEndpoint(); // 假设 Model 中有这个字段
            invokeEndpoint = setting.getInvokeEndpoint(); // 假设 Model 中有这个字段
            authorizeEndpoint = setting.getAuthorizeEndpoint(); // 假设 Model 中有这个字段
            clientId = setting.getClientId();
            clientSecret = setting.getClientSecret();
            insecureSkipVerify = setting.isInsecureSkipVerify();
            trustCaCert = setting.getTrustCaCert();
            sessionTimeoutSec = setting.getSessionTimeoutSec();

            // DEBUG: 打印加载的详细配置，但必须对敏感信息脱敏
            if (log.isDebugEnabled()) {
                log.debug("Loaded existing settings: clientId={}, introspectionEndpoint={}, hasSecret={}, sessionTimeout={}",
                        clientId, introspectionEndpoint, StringUtils.isNotBlank(clientSecret), sessionTimeoutSec);
            }
        } else {
            log.info("No existing OAuth2 Bridge settings found, initializing defaults.");
        }

        return INPUT;
    }

    @Override
    public String doExecute() throws IOException {
// 这里的检查通常由 Jira 框架处理，但保留也无妨
        if (this.command == null || this.command.isEmpty()) {
            return this.doDefault();
        }

        // INFO: 记录关键动作的开始
        log.info("Attempting to update OAuth2 Bridge settings.");

        // 逻辑处理：根据 baseEndpoint 自动填充
        if (StringUtils.isNotBlank(baseEndpoint)) {
            boolean changed = false;
            if (StringUtils.isBlank(introspectionEndpoint)) {
                introspectionEndpoint = baseEndpoint + "/oauth/introspection";
                changed = true;
            }
            if (StringUtils.isBlank(invokeEndpoint)) {
                invokeEndpoint = baseEndpoint + "oauth/revoke";
                changed = true;
            }
            if (StringUtils.isBlank(authorizeEndpoint)) {
                authorizeEndpoint = baseEndpoint + "oauth/authorize";
                changed = true;
            }

            // DEBUG: 只有在触发了自动填充逻辑时才打印
            if (changed) {
                log.debug("Auto-populated endpoints based on baseEndpoint: {}", baseEndpoint);
            }
        }

        Oauth2BridgeSetting oauth2BridgeSetting = new Oauth2BridgeSetting(
                introspectionEndpoint, baseEndpoint, invokeEndpoint, authorizeEndpoint,
                clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec
        );

        // DEBUG: 打印即将保存的对象，注意再次脱敏 Secret
        if (log.isDebugEnabled()) {
            log.debug("Saving settings: introspection={}, base={}, authorize={}, clientId={}, insecureSkipVerify={}, hasSecret={}",
                    introspectionEndpoint, baseEndpoint, authorizeEndpoint, clientId, insecureSkipVerify, StringUtils.isNotBlank(clientSecret));
        }

        try {
            settingService.updateSetting(oauth2BridgeSetting);
            // INFO: 明确的操作成功反馈
            log.info("OAuth2 Bridge settings updated successfully.");
        } catch (Exception e) {
            // ERROR: 捕获异常并记录堆栈，这是日志最重要的部分之一
            log.error("Failed to update OAuth2 Bridge settings.", e);
            addErrorMessage("Failed to save settings: " + e.getMessage());
            return ERROR;
        }

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