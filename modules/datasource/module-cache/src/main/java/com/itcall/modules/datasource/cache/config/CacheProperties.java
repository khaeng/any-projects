package com.itcall.modules.datasource.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private static final Integer DEF_CACHE_MAX_SIZE = 100;
    private static final Integer DEF_CACHE_EXFIRE_SECONDS = 60;

    public enum CacheMode {
        SINGLE, CLUSTER, SENTINEL, NONE
    }

    private CacheMode mode = CacheMode.NONE;
    private List<String> nodes;
    private String password;
    private Integer defaultMaxSize;
    private Integer defaultExpireTime;

    public CacheMode getMode() {
        return mode;
    }

    public void setMode(String mode) {
        try {
            this.mode = CacheMode.valueOf(mode.toUpperCase());
        } catch (Exception e) {
            log.error(
                    "Undefined CacheMode['app.cache.mode'] = '{}' <=== {SINGLE, CLUSTER, SENTINEL, NONE} then Using 'NONE'",
                    mode);
            this.mode = CacheMode.NONE;
        }
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDefaultMaxSize() {
        if (Objects.isNull(this.defaultMaxSize) || this.defaultMaxSize <= 0) {
            return DEF_CACHE_MAX_SIZE;
        } else {
            return this.defaultMaxSize;
        }
    }

    public void setDefaultMaxSize(Integer defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize;
    }

    public Integer getDefaultExpireTime() {
        if (Objects.isNull(this.defaultExpireTime) || this.defaultExpireTime <= 0) {
            return DEF_CACHE_EXFIRE_SECONDS;
        } else {
            return this.defaultExpireTime;
        }
    }

    public void setDefaultExpireTime(Integer defaultExpireTime) {
        this.defaultExpireTime = defaultExpireTime;
    }

}
