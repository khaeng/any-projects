package com.itcall.modules.security.session.config;

import org.springframework.context.annotation.Configuration;

// Spring Session configuration.
// If spring-session-data-redis is on classpath and spring.session.store-type=redis, it auto-configures.
// If map, it auto-configures.
// The user said: "session... uses spring provided session".
// And "session management uses cache module".
// Since 'cache' module configures RedisConnectionFactory (if Redis mode), Spring Session Redis should pick it up automatically.
// If 'cache' module configures JCache (Local), Spring Session Map is used (default fallback if no Redis).
// We just need to ensure dependency is there.
// However, proper resource config is good.

@Configuration
public class SessionConfig {
    // No manual config needed if relying on Boot auto-config + dependency presence.
    // However, may need to set store-type explicitly in application.yml
}
