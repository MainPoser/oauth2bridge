package com.bes.jira.plugins.oauth2bridge.cache;

import com.atlassian.jira.user.ApplicationUser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class TokenCache {
    private final Cache<String, ApplicationUser> cache;

    public TokenCache(long duration) {
        if (duration <= 0) {
            duration = 30 * 60;
        }
        // 使用 Guava Cache，设置最大的空闲时间，防止缓存无限膨胀
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(duration, TimeUnit.SECONDS)
                .maximumSize(10000) // 最大缓存数量
                .build();
    }

    public ApplicationUser get(String token) {
        return cache.getIfPresent(token);
    }

    public void put(String token, ApplicationUser applicationUser) {
        this.cache.put(token, applicationUser);
    }
}
