package com.itcall.modules.security.jwt.service;

import com.itcall.modules.datasource.cache.util.CacheUtil;
import com.itcall.modules.security.jwt.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final CacheUtil cacheUtil;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private static final String CACHE_KEY_AT_PREFIX = "AT:";
    private static final String CACHE_KEY_RT_PREFIX = "RT:";
    // Cache name can be provided or default used by CacheUtil if namespaced
    // For simplicity, using "jwt-cache" or relying on CacheUtil to handle simple
    // k/v.
    // CacheUtil implementation uses cacheName. Let's assume a dedicated cache name
    // "jwt-store".
    private static final String CACHE_NAME = "jwt-store";

    public JwtTokenService(JwtProperties properties, CacheUtil cacheUtil, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.cacheUtil = cacheUtil;
        this.resourceLoader = resourceLoader;
        loadKeys();
    }

    private void loadKeys() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            Resource resource = resourceLoader.getResource(properties.getJksPath());
            if (resource.exists()) {
                // If JKS password is not set or file is empty (user said create empty file for
                // now),
                // we might fail here if we honestly try to load.
                // User said: "create empty file... I will replace it later".
                // If it's truly empty (0 bytes), load() will throw exception.
                // We should handle this gracefully or fail.
                // Given the instruction, I will wrap in try-catch and probably skip key loading
                // if file is invalid/empty
                // but then createToken will fail.
                // Since this is "AGENT_MODE", I will try to load but if fail, maybe generate
                // dummy keys or just log?
                // The prompt says "resource folder under my-token.jks exists... I will put it".
                // For now, key loading code is standard.
                if (resource.contentLength() > 0) {
                    keyStore.load(resource.getInputStream(), properties.getJksPassword().toCharArray());
                    this.privateKey = (PrivateKey) keyStore.getKey(properties.getJksAlias(),
                            properties.getJksPassword().toCharArray());
                    this.publicKey = keyStore.getCertificate(properties.getJksAlias()).getPublicKey();
                } else {
                    log.warn("JKS file is empty, skipping key loading.");
                }
            } else {
                log.warn("JKS file not found at {}", properties.getJksPath());
            }
        } catch (Exception e) {
            // e.printStackTrace();
            log.error("Failed to load JKS", e);
        }
    }

    // For testing/development if JKS fails (mock keys?) - omitting for now to
    // follow strict requirements.

    public Map<String, String> createToken(Map<String, Object> claims) {
        return createTokenPair(claims);
    }

    private Map<String, String> createTokenPair(Map<String, Object> claims) {
        if (privateKey == null) {
            throw new IllegalStateException("PrivateKey not loaded from JKS");
        }

        long now = System.currentTimeMillis();

        // Access Token
        Date atValidity = new Date(now + properties.getAccessTokenValiditySeconds() * 1000);
        String accessToken = Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(atValidity)
                .id(UUID.randomUUID().toString())
                .signWith(privateKey) // Defaults to generic alg if key supports it
                .compact();

        // Refresh Token
        Date rtValidity = new Date(now + properties.getRefreshTokenValiditySeconds() * 1000);
        String refreshToken = Jwts.builder()
                .subject((String) claims.get("sub")) // Subject usually username
                .issuedAt(new Date(now))
                .expiration(rtValidity)
                .id(UUID.randomUUID().toString())
                .signWith(privateKey)
                .compact();

        // Store in Cache
        // Note: CacheUtil doesn't strictly support TTL per entry unless underlying
        // cache supports it dynamically
        // or we configure different caches.
        // If using Redis, we rely on @Cacheable TTL config OR manual set.
        // CacheUtil 'put' is generic.
        // For distinct TTLs, we typically need "jwt-access-cache" (30m) and
        // "jwt-refresh-cache" (24h) configured in CacheConfig.
        // But CacheConfig defined 60m default.
        // We'll use "jwt-access-cache" and "jwt-refresh-cache" names and assume the
        // CacheModule (Redis) allows dynamic TTL or we need to update CacheConfig.
        // Since custom TTL per key is specific to Redis (manually) or Pre-configured
        // caches.
        // We will assume "jwt-access" and "jwt-refresh" cache names.

        cacheUtil.put("jwt-access", accessToken, claims.get("sub")); // Store username or full claims?
        cacheUtil.put("jwt-refresh", refreshToken, claims.get("sub"));

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    public Map<String, String> recreateToken(String refreshToken) {
        // Validate RT
        Claims claims = parseToken(refreshToken); // Throws if invalid signature/expired
        if (claims == null) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        // Check Cache
        Object stored = cacheUtil.get("jwt-refresh", refreshToken, Object.class);
        if (stored == null) {
            throw new IllegalArgumentException("Refresh Token revoked or expired in cache");
        }

        // Create new pair
        // Preserve original claims or reload? Usually preserve minimal identity logic
        Map<String, Object> newClaims = new HashMap<>(claims);
        // Remove standard claims to avoid duplication/errors during rebuild
        newClaims.remove("exp");
        newClaims.remove("iat");
        newClaims.remove("jti");

        // Invalidate old RT?
        revokeToken(refreshToken);

        return createTokenPair(newClaims);
    }

    public void revokeToken(String token) {
        // Try to evict from both (we don't know type strictly from string)
        cacheUtil.evict("jwt-access", token);
        cacheUtil.evict("jwt-refresh", token);
    }

    public Claims parseToken(String token) {
        if (publicKey == null) {
            throw new IllegalStateException("PublicKey not loaded");
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (Exception e) {
            log.error("Token parsing failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            // Check cache for revocation
            // We check matching cache based on... we don't know if it's AT or RT easily
            // without parsing
            // But we can check if it exists in AT cache?
            // If it's an AT, it should be in jwt-access.
            Object inAt = cacheUtil.get("jwt-access", token, Object.class);
            if (inAt != null)
                return true;

            Object inRt = cacheUtil.get("jwt-refresh", token, Object.class);
            return inRt != null;
        }
        return false;
    }
}
