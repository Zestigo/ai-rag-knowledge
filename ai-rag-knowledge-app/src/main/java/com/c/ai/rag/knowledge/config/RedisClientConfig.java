package com.c.ai.rag.knowledge.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 客户端配置类，负责初始化和配置 Redis 相关的 Bean
 * 包括 Redisson 客户端和 RedisTemplate 的配置
 *
 * @author cyh
 * @date 2026/04/01
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /** Redis 配置属性 */
    @Resource
    private RedisClientConfigProperties properties;

    /**
     * 创建 ObjectMapper 实例，配置序列化相关设置
     * 支持 Java8 时间类型和类型信息保留
     *
     * @return ObjectMapper 实例
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        // 设置属性可见性，确保所有属性都能被序列化
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 关键：支持 Java8 LocalDateTime 等时间类型
        om.registerModule(new JavaTimeModule());
        // 关键：在 JSON 中保留类型信息，防止反序列化丢失对象类型
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return om;
    }

    /**
     * 初始化 Redisson 客户端
     * 用于分布式锁、AI 会话状态同步等场景
     *
     * @return RedissonClient 实例
     */
    @Bean(name = "redissonClient")
    public RedissonClient redissonClient() {
        log.info("正在初始化 Redisson 客户端，连接地址: {}:{}", properties.getHost(), properties.getPort());
        Config config = new Config();
        // 使用自定义的 ObjectMapper 初始化序列化器
        config.setCodec(new JsonJacksonCodec(createObjectMapper()));

        config
                .useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive());

        return Redisson.create(config);
    }

    /**
     * 配置 RedisTemplate
     * Spring 生态通用，用于简单的 KV 存储
     * 确保与 Redisson 使用相同的序列化方案，达到数据互通
     *
     * @param factory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        log.info("正在配置 RedisTemplate");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper om = createObjectMapper();
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // Key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 使用统一的 Jackson 序列化
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}