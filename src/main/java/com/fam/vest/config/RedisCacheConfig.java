package com.fam.vest.config;

import java.time.Duration;
import java.util.List;

import com.fam.vest.dto.response.IpoData;
import com.fam.vest.dto.response.IpoResponse;
import com.fam.vest.pojo.MutualFundNav;
import com.fam.vest.pojo.email.ResendEmailPayload;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))     // daily TTL
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> method.getName() + "::" + params[0];
    }

    @Bean
    public RedisTemplate<String, ResendEmailPayload> resendEmailPayloadRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ResendEmailPayload> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ResendEmailPayload.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, MutualFundNav> mutualFundNavRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, MutualFundNav> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(MutualFundNav.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, IpoResponse> ipoRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, IpoResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(IpoResponse.class));
        return template;
    }
}
