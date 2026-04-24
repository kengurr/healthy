package com.zdravdom.matching.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson client for distributed Redis locks (slot locking).
 * Configured from application.yml data.redis settings.
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(16);
        } else {
            config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(16);
        }
        return Redisson.create(config);
    }
}
