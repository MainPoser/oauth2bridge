package com.bes.jira.plugins.oauth2bridge.store;

import com.bes.jira.plugins.oauth2bridge.servlet.filter.Oauth2BridgeServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 线程安全的 TTL (Time-to-Live) 缓存，适用于存储 OAuth State 等短期数据。
 * 特点：
 * 1. 线程安全 (ConcurrentHashMap)。
 * 2. 支持获取时删除 (getAndRemove)。
 * 3. 支持后台定期自动清理过期项。
 *
 * @param <K> 键类型 (通常是 String, 即 state 字符串)
 * @param <V> 值类型 (通常是用户 ID 或重定向 URL)
 */
public class StateTtlCache<K, V> {
    private static final Logger log = LoggerFactory.getLogger(StateTtlCache.class);

    // 内部类：缓存项，存储实际的值和过期时间
    private static class CacheEntry<V> {
        final V value;
        final long expirationTime; // 绝对过期时间戳 (System.currentTimeMillis() + TTL)

        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
        }

        /**
         * 检查当前缓存项是否已过期。
         *
         * @return 如果当前时间超过了 expirationTime，返回 true。
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    // 核心存储结构：线程安全的 Map
    private final ConcurrentHashMap<K, CacheEntry<V>> cacheMap = new ConcurrentHashMap<>();

    // 定时任务服务，用于定期清理后台
    private final ScheduledExecutorService cleanupScheduler;

    /**
     * 构造函数。初始化并启动后台清理服务。
     *
     * @param cleanupIntervalSeconds 清理线程的间隔时间（秒）。
     * @param initialDelaySeconds    首次执行清理的延迟时间（秒）。
     */
    public StateTtlCache(long initialDelaySeconds, long cleanupIntervalSeconds) {
        // 创建一个单线程调度服务
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "StateCache-Cleanup-Thread") // 给予线程一个有意义的名字
        );

        // 调度定期任务
        this.cleanupScheduler.scheduleWithFixedDelay(
                this::cleanUpExpiredEntries,
                initialDelaySeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
        );
        log.info("State Cache Auto Clean Started，Interval: {}s.", cleanupIntervalSeconds);
    }

    /**
     * 将一个键值对放入缓存，并指定其存活时间 (TTL)。
     *
     * @param key       键 (OAuth State)
     * @param value     值 (例如，用户的 Account ID 或其他相关数据)
     * @param ttlMillis 存活时间（毫秒）。
     */
    public void put(K key, V value, long ttlMillis) {
        if (key == null || value == null || ttlMillis <= 0) {
            return;
        }
        CacheEntry<V> entry = new CacheEntry<>(value, ttlMillis);
        cacheMap.put(key, entry);
    }

    /**
     * 从缓存中获取一个值。
     * 1. 检查是否过期。如果过期，则删除并返回 null (惰性清理)。
     * 2. 如果未过期，则返回其值并立即将其从缓存中删除（获取时删除）。
     *
     * @param key 键
     * @return 对应的值，如果键不存在或已过期，则返回 null。
     */
    public V getAndRemove(K key) {
        if (key == null) {
            return null;
        }

        // 使用 computeIfPresent 确保操作的原子性，但实现获取时删除，
        // 简单使用 remove 然后检查更直观和符合 State 需求。
        CacheEntry<V> entry = cacheMap.remove(key);

        if (entry == null) {
            return null;
        }

        // 检查过期时间
        if (entry.isExpired()) {
            // 已经过期，返回 null。它已被 remove 移除。
            return null;
        } else {
            // 未过期，返回其值。它已被 remove 移除。
            return entry.value;
        }
    }

    /**
     * 后台自动清理方法。
     * 遍历所有缓存项，删除已过期的。
     */
    private void cleanUpExpiredEntries() {
        if (cacheMap.isEmpty()) {
            return;
        }

        Predicate<Map.Entry<K, CacheEntry<V>>> isExpired = entry -> entry.getValue().isExpired();

        int sizeBefore = cacheMap.size();

        // removeIf 遍历并移除所有满足条件的 Entry
        // Java 8 之后，Map 接口支持 removeIf，但 ConcurrentHashMap 没有直接实现这个方法。
        // 我们使用 entrySet().removeIf()，它在 ConcurrentHashMap 中是线程安全的。
        cacheMap.entrySet().removeIf(entry -> entry.getValue().isExpired());

        int cleanedCount = sizeBefore - cacheMap.size();
        if (cleanedCount > 0) {
            log.info("State Cache Clean {} Expired Entry.", cleanedCount);
            // 在实际 JIRA 插件中，使用 JIRA 的日志系统代替 System.out.println
            // System.out.println("State Cache 后台清理了 " + cleanedCount + " 个过期项。");
        }
    }

    /**
     * 必须在插件卸载或停止时调用此方法，以优雅地关闭后台线程。
     * 在 P2 插件中，这通常在你的 Component 的 destroy 方法中调用。
     */
    public void shutdown() {
        if (!cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdownNow(); // 立即尝试停止所有任务
            // 在实际插件中，最好加上 try-catch 块处理 InterruptedException
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.info("State Cache Clean Thread Not Termination in 5 sec.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 重新设置中断状态
            }
            System.out.println("State Cache 自动清理服务已停止。");
        }
    }

    public int size() {
        return cacheMap.size();
    }
}
