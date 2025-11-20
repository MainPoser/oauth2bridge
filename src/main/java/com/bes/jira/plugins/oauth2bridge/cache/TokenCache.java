package com.bes.jira.plugins.oauth2bridge.cache;

import com.atlassian.jira.user.ApplicationUser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class TokenCache {
    private Cache<String, ApplicationUser> cache;
    private long duration;

    public TokenCache(long duration) {
        if (duration <= 0) {
            duration = 30 * 60;
        }
        this.duration = duration;
        // 使用 Guava Cache，设置最大的空闲时间，防止缓存无限膨胀
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(this.duration, TimeUnit.SECONDS).maximumSize(10000) // 最大缓存数量
                .build();
    }

    public ApplicationUser get(String token) {
        return cache.getIfPresent(token);
    }

    public void put(String token, ApplicationUser applicationUser) {
        this.cache.put(token, applicationUser);
    }

    public long getDuration() {
        return duration;
    }

    // 当需要修改过期时间时：
    public void rebuildCacheWithNewExpire(long newExpireSec) {
        Cache<String, ApplicationUser> oldCache = cache;

        // 1. 创建新的 Cache
        Cache<String, ApplicationUser> newCache = CacheBuilder.newBuilder().expireAfterWrite(newExpireSec, TimeUnit.SECONDS).build();

        // 2. 把旧数据迁移到新 Cache（尽最大可能）
        newCache.putAll(oldCache.asMap());

        // 3. 用新的 Cache 替换引用
        cache = newCache;
        duration = newExpireSec;

        // 4.（可选）清理旧 Cache（让 GC 收走）
        oldCache.invalidateAll();
    }
}
