package com.bonc.demo.drools.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

    @Value("${redis.server}")
    private String server;

    @Value("${redis.port}")
    private Integer port;

    @Value("${redis.password}")
    private String password;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(server, port);
        if (StringUtils.isNoneBlank(password)) {
            redisStandaloneConfiguration.setPassword(password);
        }

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }
}
