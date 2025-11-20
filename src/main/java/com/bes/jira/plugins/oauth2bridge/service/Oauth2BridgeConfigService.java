package com.bes.jira.plugins.oauth2bridge.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Named
public class Oauth2BridgeConfigService {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeConfigService.class);

    // 使用 AtomicReference 存储当前的配置对象快照。
    private final AtomicReference<Oauth2BridgeConfig> configCache;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PluginSettings pluginSettings;
    // 插件存储的唯一 key，用于隔离配置
    private static final String PLUGIN_STORAGE_KEY = "com.bes.jira.plugins.oauth2bridge.";

    public static String KEY_INTROSPECTION_ENDPOINT = "introspectionEndpoint";
    public static String KEY_CLIENT_ID = "clientId";
    public static String KEY_CLIENT_SECRET = "clientSecret";
    public static String KEY_INSECURE_SKIP_VERIFY = "insecureSkipVerify";
    public static String KEY_TRUST_CA_CERT = "trustCaCert";
    public static String KEY_SESSION_TIMEOUT_SEC = "sessionTimeoutSec";

    @Inject
    public Oauth2BridgeConfigService(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        // 插件启动时，从持久化存储加载初始配置并初始化 AtomicReference
        Oauth2BridgeConfig initialConfig = loadConfigFromPersistence();
        this.configCache = new AtomicReference<>(initialConfig);
    }

    public void saveConfig(Oauth2BridgeConfig oauth2BridgeConfig) throws IOException {
        // 1. 将新值持久化到存储中 (略过细节)
        persistConfig(oauth2BridgeConfig);

        // 3. 原子性地替换缓存中的引用。
        configCache.set(oauth2BridgeConfig);

        log.info("Configuration cache updated successfully. InsecureSkipVerify: {}", oauth2BridgeConfig);
    }

    public Oauth2BridgeConfig getConfig() {
        return configCache.get();
    }

    // --- 内部辅助方法 (持久化和加载) ---
    private Oauth2BridgeConfig loadConfigFromPersistence() {
        // 从 PluginSettings 或其他存储中读取当前值
        Object o = pluginSettings.get(PLUGIN_STORAGE_KEY);
        if (o == null) {
            log.debug("Oauth2BridgeConfig is empty, use default.");
            return new Oauth2BridgeConfig("", "", "", true, "", 30 * 60);
        }
        try {
            return mapper.readValue((String) o, Oauth2BridgeConfig.class);
        } catch (IOException e) {
            log.error("Parse Oauth2BridgeConfig Failed: {}, use default.", e.getMessage());
            return new Oauth2BridgeConfig("", "", "", true, "", 30 * 60);
        }

    }

    private void persistConfig(Oauth2BridgeConfig oauth2BridgeConfig) throws IOException {
        // 将新值写入 PluginSettings
        String o2cStr = mapper.writeValueAsString(oauth2BridgeConfig);
        pluginSettings.put(PLUGIN_STORAGE_KEY, o2cStr);
    }
}
