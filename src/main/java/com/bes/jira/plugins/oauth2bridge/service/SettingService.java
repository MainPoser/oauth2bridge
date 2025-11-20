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
    public SettingService(@ComponentImport PluginSettingsFactory pluginSettingsFactory, @ComponentImport ModuleDescriptor<?> moduleDescriptor) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.pluginKey = "com.bes.jira.plugins.oauth2bridge.settings";
        // 插件启动时，从持久化存储加载初始配置并初始化 AtomicReference
        Oauth2BridgeSetting initialSetting = loadSettingFromPersistence();
        this.settingCache = new AtomicReference<>(initialSetting);
    }

    public void updateSetting(Oauth2BridgeSetting oauth2BridgeSetting) throws IOException {
        // 1. 将新值持久化到存储中 (略过细节)
        persistSetting(oauth2BridgeSetting);

        // 3. 原子性地替换缓存中的引用。
        settingCache.set(oauth2BridgeSetting);

        log.info("Setting cache updated successfully: {}", oauth2BridgeSetting.toString());
    }

    public Oauth2BridgeSetting getSetting() {
        return settingCache.get();
    }

    public void removeSetting() {
        pluginSettings.remove(pluginKey);
    }

    // --- 内部辅助方法 (持久化和加载) ---
    private Oauth2BridgeSetting loadSettingFromPersistence() {
        // 从 PluginSettings 或其他存储中读取当前值
        Object settingStr = pluginSettings.get(pluginKey);
        if (settingStr == null) {
            log.debug("Oauth2BridgeSetting is empty, use default.");
            return new Oauth2BridgeSetting("", "", "", true, "", 30 * 60);
        }
        try {
            log.debug("Oauth2BridgeSetting str persistence is {}.", settingStr);
            return mapper.readValue((String) settingStr, Oauth2BridgeSetting.class);
        } catch (IOException e) {
            log.error("Parse Oauth2BridgeSetting Failed: {}, use default.", e.getMessage());
            return new Oauth2BridgeSetting("", "", "", true, "", 30 * 60);
        }

    }

    private void persistSetting(Oauth2BridgeSetting Oauth2BridgeSetting) throws IOException {
        // 将新值写入 PluginSettings
        String o2cStr = mapper.writeValueAsString(Oauth2BridgeSetting);
        pluginSettings.put(pluginKey, o2cStr);
    }
}
