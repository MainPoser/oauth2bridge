package com.bes.jira.plugins.oauth2bridge.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginUninstalledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Oauth2BridgeEventListener {
    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeEventListener.class);
    private final String pluginKey = "com.bes.jira.plugins.oauth2bridge"; // 插件Key

    @Inject
    public Oauth2BridgeEventListener(@ComponentImport EventPublisher eventPublisher) {
        // 在 Jira/Confluence 中，您需要通过 Atlassian Plugins Framework 获取插件 Key
        // 这里假设您的插件Key是固定的，或者您通过其他注入方式获取（如 PluginInformation）

        // 确保在实例化时注册到 EventPublisher
        eventPublisher.register(this);
        log.debug("Oauth2BridgeEventListener registered for events.");
    }

    @EventListener
    public void onPluginUninstalled(PluginUninstalledEvent event) {
        // 检查事件是否针对我们自己的插件
        if (pluginKey.equals(event.getPlugin().getKey())) {
            log.debug("Oauth2BridgeEventListener onPluginUninstalled {}", pluginKey);
        }
    }

// -----------------------------------------------------------------
    // 可选：监听 Disabled 事件 (如果需要区分 onStop() 的两种可能)
    // -----------------------------------------------------------------

    @EventListener
    public void onPluginDisabled(PluginDisabledEvent event) {
        if (pluginKey.equals(event.getPlugin().getKey())) {
            // 这个事件在 onStop() 之前或同时发生，如果您想精确记录或区分，可以使用
            log.debug("Oauth2BridgeEventListener onPluginDisabled {}", pluginKey);
        }
    }
}
