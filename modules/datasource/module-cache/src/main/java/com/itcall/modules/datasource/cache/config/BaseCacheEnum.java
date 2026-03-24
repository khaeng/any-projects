package com.itcall.modules.datasource.cache.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BaseCacheEnum implements CacheEnum {

    HOUR_CACHE("CACHE:BASE:HOUR", 200, 3600),
    HALF_HOUR_CACHE("CACHE:BASE:ONE_HOUR", 200, 1800),
    TEN_MIN_CACHE("CACHE:BASE:TEN_MIN", 200, 600),
    FIVE_MIN_CACHE("CACHE:BASE:FIVE_MIN", 200, 300),
    ONE_MIN_CACHE("CACHE:BASE:ONE_MIN", 200, 60);

    private final String cacheName;
    private final long maxSize;
    private final long expireTime;
}
