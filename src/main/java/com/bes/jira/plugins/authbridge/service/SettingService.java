package com.bes.jira.plugins.authbridge.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.bes.jira.plugins.authbridge.model.AuthBridgeSetting;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Named
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    // 使用 AtomicReference 存储当前的配置对象快照。
    private final AtomicReference<AuthBridgeSetting> settingCache;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PluginSettings pluginSettings;
    // 插件存储的唯一 key，用于隔离配置
    private final String pluginKey;

    @Inject
    public SettingService(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.pluginKey = "com.bes.jira.plugins.authbridge.settings";

        // INFO: 记录服务启动，准备加载配置
        log.info("Initializing SettingService. Loading configuration from persistence.");

        // 插件启动时，从持久化存储加载初始配置并初始化 AtomicReference
        AuthBridgeSetting initialSetting = loadSettingFromPersistence();
        this.settingCache = new AtomicReference<>(initialSetting);

        // INFO: 记录初始配置加载完成的摘要
        log.info("Initial settings loaded.");
    }

    public void updateSetting(AuthBridgeSetting authBridgeSetting) throws IOException {
        // 1. 将新值持久化到存储中
        persistSetting(authBridgeSetting);

        // 2. 原子性地替换缓存中的引用。
        settingCache.set(authBridgeSetting);

        // INFO: 记录关键操作成功。注意：必须脱敏 clientSecret 和 trustCaCert。
        log.info("Settings updated and cache replaced successfully. Details: Skip Verify={}, Has Custom CA={}",
                authBridgeSetting.isInsecureSkipVerify(),
                (authBridgeSetting.getTrustCaCert() != null && !authBridgeSetting.getTrustCaCert().isEmpty()) // 检查证书是否存在
        );
    }

    public AuthBridgeSetting getSetting() {
        return settingCache.get();
    }

    public void removeSetting() {
        pluginSettings.remove(pluginKey);
        log.info("Plugin settings removed for key: {}", pluginKey);
    }

    // --- 内部辅助方法 (持久化和加载) ---
    private AuthBridgeSetting loadSettingFromPersistence() {
        // 从 PluginSettings 或其他存储中读取当前值
        Object settingStr = pluginSettings.get(pluginKey);

        if (settingStr == null) {
            log.info("AuthBridgeSetting persistence is empty, initializing default configuration.");
            return createDefaultSetting();
        }

        // DEBUG: 记录读取到的原始配置字符串（不包含敏感信息，因为持久化时已经转换为字符串，但仍然是配置的完整快照）
        log.debug("AuthBridgeSetting raw persistence string loaded: {}", settingStr);

        try {
            AuthBridgeSetting loadedSetting = mapper.readValue((String) settingStr, AuthBridgeSetting.class);
            // DEBUG: 记录加载成功的配置摘要（脱敏）
            log.debug("Successfully parsed settings: Skip Verify={}, Has Custom CA={}",
                    loadedSetting.isInsecureSkipVerify(),
                    (loadedSetting.getTrustCaCert() != null && !loadedSetting.getTrustCaCert().isEmpty()));

            return loadedSetting;

        } catch (IOException e) {
            // ERROR: 配置解析失败是严重问题
            log.error("Failed to parse AuthBridgeSetting from persistence. Using default settings.", e);
            return createDefaultSetting();
        }

    }

    private AuthBridgeSetting createDefaultSetting() {
        return new AuthBridgeSetting(new ArrayList<>(), true, "");
    }

    private void persistSetting(AuthBridgeSetting authBridgeSetting) throws IOException {
        // 将新值写入 PluginSettings
        String o2cStr = mapper.writeValueAsString(authBridgeSetting);

        // DEBUG: 记录即将持久化的字符串
        log.debug("Persisting new settings to storage: {}", o2cStr);

        pluginSettings.put(pluginKey, o2cStr);
    }
}
