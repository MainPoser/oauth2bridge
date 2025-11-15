package com.bes.jira.plugins.oauth2bridge.store;

import javax.inject.Named;

@Named()
public class StateCache {
    private final StateTtlCache<String, String> cache;
    private static final long CLEANUP_INTERVAL_SECONDS = 300; // 5分钟清理一次
    private static final long STATE_TTL_MS = 60000; // OAuth State 通常只有 1 分钟有效

    public StateCache() {
        // 首次延迟 60 秒，之后每 300 秒（5分钟）清理一次
        this.cache = new StateTtlCache<>(60, CLEANUP_INTERVAL_SECONDS);
    }

    // 暴露给其他组件使用的方法
    public void storeState(String state, String userData) {
        cache.put(state, userData, STATE_TTL_MS);
    }

    public String retrieveAndRemoveState(String state) {
        return cache.getAndRemove(state);
    }
}
