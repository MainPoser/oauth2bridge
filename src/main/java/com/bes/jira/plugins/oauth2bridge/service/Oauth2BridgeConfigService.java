package com.bes.jira.plugins.oauth2bridge.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Oauth2BridgeConfigService {

    private final PluginSettings pluginSettings;
    // 插件存储的唯一 key，用于隔离配置
    private static final String PLUGIN_STORAGE_KEY = "com.bes.jira.plugins.oauth2bridge.";

    public static String KEY_CLIENT_ID = "clientId";
    public static String KEY_CLIENT_SECRET = "clientSecret";
    public static String KEY_AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
    public static String KEY_TOKEN_ENDPOINT = "tokenEndpoint";
    public static String KEY_USERINFO_ENDPOINT = "userInfoEndpoint";

    @Inject
    public Oauth2BridgeConfigService(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public void saveConfig(String key, String value) {
        pluginSettings.put(PLUGIN_STORAGE_KEY + key, value);
    }

    public String getConfig(String key) {
        return (String) pluginSettings.get(PLUGIN_STORAGE_KEY + key);
    }
}
