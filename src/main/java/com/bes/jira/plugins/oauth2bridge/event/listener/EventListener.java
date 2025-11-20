package com.bes.jira.plugins.oauth2bridge.event.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.*;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);
    private final String pluginKey; // 插件Key

    private final SettingService oauth2BridgeSettingService;

    @Inject
    public EventListener(@ComponentImport EventPublisher eventPublisher, SettingService settingService) {
        pluginKey = "com.bes.jira.plugins.oauth2bridge";
        // 确保在实例化时注册到 EventPublisher
        eventPublisher.register(this);
        this.oauth2BridgeSettingService = settingService;
    }

    @com.atlassian.event.api.EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginEnabled");
        }
    }

    @com.atlassian.event.api.EventListener
    public void onPluginDisabling(PluginDisablingEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginDisabling");
        }
    }

    @com.atlassian.event.api.EventListener
    public void onPluginDisabled(PluginDisabledEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginDisabled");
        }
    }

    @com.atlassian.event.api.EventListener
    public void onPluginUninstalling(PluginUninstallingEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginUninstalling {}, remove setting", pluginKey);
            oauth2BridgeSettingService.removeSetting();
        }
    }

    @com.atlassian.event.api.EventListener
    public void onPluginUninstalled(PluginUninstalledEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginUninstalled");
        }
    }
}
