package com.itcall.modules.datasource.cache.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CacheUtil {

    private final CacheManager cacheManager;

    public CacheUtil(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void put(String cacheName, Object key, Object value) {
        getCache(cacheName).ifPresent(cache -> cache.put(key, value));
    }

    public <T> T get(String cacheName, Object key, Class<T> type) {
        return getCache(cacheName)
                .map(cache -> cache.get(key, type))
                .orElse(null);
    }

    public void evict(String cacheName, Object key) {
        getCache(cacheName).ifPresent(cache -> cache.evict(key));
    }

    public void clear(String cacheName) {
        getCache(cacheName).ifPresent(Cache::clear);
    }

    private Optional<Cache> getCache(String cacheName) {
        return Optional.ofNullable(cacheManager.getCache(cacheName));
    }
}
