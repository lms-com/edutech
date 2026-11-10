package com.lms.course.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_COURSE_DETAIL = "course_detail";
    public static final String CACHE_PUBLISHED_COURSES = "published_courses";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Cấu hình serializer chuẩn cho Redis hỗ trợ đầy đủ polymorphic typing (@class)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // Tùy biến TTL riêng cho từng vùng Cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // Danh mục rất ít khi thay đổi -> cache 60 phút
        cacheConfigurations.put(CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofHours(1)));
        // Chi tiết khóa học -> cache 15 phút
        cacheConfigurations.put(CACHE_COURSE_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        // Danh sách khóa học trang chủ -> cache 10 phút
        cacheConfigurations.put(CACHE_PUBLISHED_COURSES, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
