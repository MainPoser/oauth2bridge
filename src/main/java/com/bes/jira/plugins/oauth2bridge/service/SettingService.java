package com.bes.jira.plugins.oauth2bridge.service;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeSetting;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Named
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    // 使用 AtomicReference 存储当前的配置对象快照。
    private final AtomicReference<Oauth2BridgeSetting> settingCache;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PluginSettings pluginSettings;
    // 插件存储的唯一 key，用于隔离配置
    private final String pluginKey;

    @Inject
    public SettingService(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.pluginKey = "com.bes.jira.plugins.oauth2bridge.settings";

        // INFO: 记录服务启动，准备加载配置
        log.info("Initializing SettingService. Loading configuration from persistence.");

        // 插件启动时，从持久化存储加载初始配置并初始化 AtomicReference
        Oauth2BridgeSetting initialSetting = loadSettingFromPersistence();
        this.settingCache = new AtomicReference<>(initialSetting);

        // INFO: 记录初始配置加载完成的摘要
        log.info("Initial settings loaded. Client ID: {}, Base Endpoint: {}, Session Timeout: {}s.",
                initialSetting.getClientId(), initialSetting.getBaseEndpoint(), initialSetting.getSessionTimeoutSec());
    }

    public void updateSetting(Oauth2BridgeSetting oauth2BridgeSetting) throws IOException {
        // 1. 将新值持久化到存储中
        persistSetting(oauth2BridgeSetting);

        // 2. 原子性地替换缓存中的引用。
        settingCache.set(oauth2BridgeSetting);

        // INFO: 记录关键操作成功。注意：必须脱敏 clientSecret 和 trustCaCert。
        log.info("Settings updated and cache replaced successfully. Details: Client ID={}, Base Endpoint={}, Skip Verify={}, Has Secret={}, Has Custom CA={}",
                oauth2BridgeSetting.getClientId(),
                oauth2BridgeSetting.getBaseEndpoint(),
                oauth2BridgeSetting.isInsecureSkipVerify(),
                (oauth2BridgeSetting.getClientSecret() != null && !oauth2BridgeSetting.getClientSecret().isEmpty()), // 检查 Secret 是否存在
                (oauth2BridgeSetting.getTrustCaCert() != null && !oauth2BridgeSetting.getTrustCaCert().isEmpty()) // 检查证书是否存在
        );
    }

    public Oauth2BridgeSetting getSetting() {
        return settingCache.get();
    }

    public void removeSetting() {
        pluginSettings.remove(pluginKey);
        log.info("Plugin settings removed for key: {}", pluginKey);
    }

    // --- 内部辅助方法 (持久化和加载) ---
    private Oauth2BridgeSetting loadSettingFromPersistence() {
        // 从 PluginSettings 或其他存储中读取当前值
        Object settingStr = pluginSettings.get(pluginKey);

        if (settingStr == null) {
            log.info("Oauth2BridgeSetting persistence is empty, initializing default configuration.");
            return createDefaultSetting();
        }

        // DEBUG: 记录读取到的原始配置字符串（不包含敏感信息，因为持久化时已经转换为字符串，但仍然是配置的完整快照）
        log.debug("Oauth2BridgeSetting raw persistence string loaded. Length: {}", settingStr.toString().length());

        try {
            Oauth2BridgeSetting loadedSetting = mapper.readValue((String) settingStr, Oauth2BridgeSetting.class);
            // DEBUG: 记录加载成功的配置摘要（脱敏）
            log.debug("Successfully parsed settings: Client ID={}, Skip Verify={}, Has Secret={}, Has Custom CA={}",
                    loadedSetting.getClientId(),
                    loadedSetting.isInsecureSkipVerify(),
                    (loadedSetting.getClientSecret() != null && !loadedSetting.getClientSecret().isEmpty()),
                    (loadedSetting.getTrustCaCert() != null && !loadedSetting.getTrustCaCert().isEmpty()));

            return loadedSetting;

        } catch (IOException e) {
            // ERROR: 配置解析失败是严重问题
            log.error("Failed to parse Oauth2BridgeSetting from persistence. Using default settings.", e);
            return createDefaultSetting();
        }

    }

    private Oauth2BridgeSetting createDefaultSetting() {
        return new Oauth2BridgeSetting(
                "", "", "", "",
                "", "", true, "", 30 * 60
        );
    }

    private void persistSetting(Oauth2BridgeSetting Oauth2BridgeSetting) throws IOException {
        // 将新值写入 PluginSettings
        String o2cStr = mapper.writeValueAsString(Oauth2BridgeSetting);

        // DEBUG: 记录即将持久化的字符串长度，但绝不记录内容
        log.debug("Persisting new settings to storage. String length: {}", o2cStr.length());

        pluginSettings.put(pluginKey, o2cStr);
    }
}
