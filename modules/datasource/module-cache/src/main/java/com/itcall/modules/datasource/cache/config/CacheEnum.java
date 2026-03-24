package com.itcall.modules.datasource.cache.config;

public interface CustomCacheEnum {

    /**
     * 캐시를 적용할 PreFix 이름
     * 
     * @return
     */
    public String getCacheName();

    /**
     * 메모리 최대 적재 크기
     * 
     * @return
     */
    public long getMaxSize();

    /**
     * 캐시 유효시간(초)
     * 
     * @return
     */
    public long getExpireTime();

}
