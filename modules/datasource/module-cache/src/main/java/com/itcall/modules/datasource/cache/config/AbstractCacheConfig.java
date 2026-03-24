package com.itcall.modules.datasource.cache.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캐시설정을 진행한다.
 * 1. Redis가 연결되어 있으면 Redis 캐시로 설정한다.
 * 2. Redis가 아니라면 로컬캐시로 설정한다.
 * 3. 캐시매니저는 기본적으로 제공되는 BaseCacheEnum을 만든다.
 * 4. 상속받은 곳에서 추가 캐시를 설정하고 싶으면, @AbstractCacheConfig.class를
 * 상속해서 @Configuration, @EnableCaching를 선언하여 getCaches()를 제구현 해야 한다.
 * - 재구현 시 CacheEnum을 상속받아 추가할 캐시를 먼저 정의한 후 해당 Enum을 Values()로 반환해주면 된다.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@ConditionalOnMissingBean(AbstractCacheConfig.class)
@EnableCaching
public class AbstractCacheConfig {

    private final CacheProperties properties;
    private final List<RedisCacheManagerBuilderCustomizer> customizers;

    /**
     * [Redis 전용 Customizer Bean]
     * SINGLE, CLUSTER, SENTINEL 중 하나일 때만 생성 (대소문자 무관)
     */
    @Bean
    @ConditionalOnExpression("#{['single', 'cluster', 'sentinel'].contains('${app.cache.mode:none}'.toLowerCase())}")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilder() {
        return builder -> {
            // 모듈내에서 기본적으로 지원하는 캐시 정책을 로드한다.
            Arrays.stream(BaseCacheEnum.values()).forEach(cache -> builder.withCacheConfiguration(
                    cache.getCacheName(),
                    RedisCacheConfiguration.defaultCacheConfig()
                            .serializeValuesWith(RedisSerializationContext.SerializationPair
                                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                            .entryTtl(Duration.ofSeconds(cache.getExpireTime()))
                            .disableCachingNullValues()));

            // 상속받은 프로젝트에서 별도로 선언한 캐시 정책을 로드한다.
            getCaches().forEach(cache -> builder.withCacheConfiguration(cache.getCacheName(),
                    RedisCacheConfiguration.defaultCacheConfig()
                            .serializeValuesWith(RedisSerializationContext.SerializationPair
                                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                            .entryTtl(Duration.ofSeconds(cache.getExpireTime()))
                            .disableCachingNullValues()));
        };
    }

    @Bean
    public CacheManager cacheManager() {
        switch (properties.getMode()) {
            case SINGLE:
            case CLUSTER:
            case SENTINEL:
                return createRedisCacheManager();
            case NONE:
            default:
                return createLocalCacheManager();
        }
    }

    private CacheManager createRedisCacheManager() {
        RedisConnectionFactory connectionFactory = createRedisConnectionFactory();
        ((LettuceConnectionFactory) connectionFactory).afterPropertiesSet();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(this.properties.getDefaultExpireTime()))
                .disableCachingNullValues();

        RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config); // 기본 캐시 정책 적용.

        // 사전 정의된 Cache Prefix에 대해서 별도 정책을 적용한다.
        this.customizers.forEach(customizer -> customizer.customize(builder));

        return builder.build();
    }

    private RedisConnectionFactory createRedisConnectionFactory() {
        if (properties.getNodes() == null || properties.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Redis nodes must be configured for mode " + properties.getMode());
        }

        switch (properties.getMode()) {
            case SINGLE:
                String[] parts = properties.getNodes().get(0).split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
                if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
                    standaloneConfig.setPassword(properties.getPassword());
                }
                return new LettuceConnectionFactory(standaloneConfig);

            case CLUSTER:
                RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(properties.getNodes());
                if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
                    clusterConfig.setPassword(properties.getPassword());
                }
                return new LettuceConnectionFactory(clusterConfig);

            case SENTINEL:
                RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                        .master("master");

                for (String node : properties.getNodes()) {
                    String[] sParts = node.split(":");
                    sentinelConfig.sentinel(sParts[0], Integer.parseInt(sParts[1]));
                }
                if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
                    sentinelConfig.setPassword(properties.getPassword());
                }
                return new LettuceConnectionFactory(sentinelConfig);

            default:
                throw new IllegalArgumentException("Invalid Redis Mode");
        }
    }

    private CacheManager createLocalCacheManager() {
        CachingProvider provider = Caching.getCachingProvider();
        javax.cache.CacheManager jsr107Manager = provider.getCacheManager();

        // 1. 정의되지 않은 캐시를 위한 "기본 설정(Default)" 객체
        org.ehcache.config.CacheConfiguration<Object, Object> defaultEhcacheConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Object.class, Object.class,
                        ResourcePoolsBuilder.heap(this.properties.getDefaultMaxSize()))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
                        Duration.ofMinutes(this.properties.getDefaultExpireTime())))
                .build();

        // 2. 미리 정의된 캐시 정책 (BaseCacheEnum + getCaches) 먼저 등록
        Arrays.stream(BaseCacheEnum.values()).forEach(cache -> registerJsr107Cache(jsr107Manager, cache));
        getCaches().forEach(cache -> registerJsr107Cache(jsr107Manager, cache));

        // 3. JCacheCacheManager를 확장하여 "정의되지 않은 캐시" 처리
        return new JCacheCacheManager(jsr107Manager) {
            @Override
            protected org.springframework.cache.Cache getMissingCache(String name) {
                // @Cacheable("undefinedName") 처럼 미리 등록되지 않은 캐시가 호출되면 이 로직이 실행됨
                synchronized (jsr107Manager) {
                    // 더블 체크: 그 사이 다른 스레드에서 생성했을 수 있음
                    javax.cache.Cache<Object, Object> jCache = jsr107Manager.getCache(name);

                    if (jCache == null) {
                        // 기본 설정을 사용하여 즉석에서 캐시 생성
                        jsr107Manager.createCache(name,
                                Eh107Configuration.fromEhcacheCacheConfiguration(defaultEhcacheConfig));
                    }
                }
                return super.getCache(name);
            }
        };
    }

    /**
     * 미리 정의된 정책으로 캐시를 등록하는 헬퍼 메서드 (기존 유지)
     */
    private void registerJsr107Cache(javax.cache.CacheManager jsr107Manager, CacheEnum cache) {
        if (jsr107Manager.getCache(cache.getCacheName()) == null) {
            org.ehcache.config.CacheConfiguration<Object, Object> cacheConfig = CacheConfigurationBuilder
                    .newCacheConfigurationBuilder(Object.class, Object.class,
                            ResourcePoolsBuilder.heap(cache.getMaxSize()))
                    .withExpiry(ExpiryPolicyBuilder
                            .timeToLiveExpiration(Duration.ofSeconds(cache.getExpireTime())))
                    .build();

            jsr107Manager.createCache(cache.getCacheName(),
                    Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfig));
        }
    }

    /**
     * 이부분을 재구현하여 새로운 캐시정책을 추가하면 된다.
     * 상속 재구현 클래스에서 @Configuraton과 @EnableCaching을 선언하여 구현하여야 한다.
     * 본 메소드에서 추가 생성된 캐시Enum을 반환해주면 된다.
     * Ex) return List.of(BaseCacheEnum.values());
     * 
     * @return
     */
    protected List<CacheEnum> getCaches() {
        log.warn("재 구현된 CustomCacheCustomizer가 없으며, 기본캐시만 적용합니다. 기본캐시: {}",
                Arrays.asList(BaseCacheEnum.values()).stream().map(c -> c.getCacheName()).collect(Collectors.toList()));
        return List.of();
    }
}
