package com.c.ai.rag.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 客户端配置属性类
 * 用于读取和管理 Redis 相关的配置参数
 *
 * @author cyh
 * @date 2026/04/01
 */
@Data
@ConfigurationProperties(prefix = "redis.sdk.config", ignoreInvalidFields = true)
public class RedisClientConfigProperties {

    /** Redis 主机地址 */
    private String host;
    /** Redis 端口 */
    private int port;
    /** Redis 密码 */
    private String password;
    /** 连接池大小，默认为64 */
    private int poolSize = 64;
    /** 连接池最小空闲连接数，默认为10 */
    private int minIdleSize = 10;
    /** 连接最大空闲时间（单位：毫秒），超过该时间的空闲连接将被关闭，默认为10000 */
    private int idleTimeout = 10000;
    /** 连接超时时间（单位：毫秒），默认为10000 */
    private int connectTimeout = 10000;
    /** 连接重试次数，默认为3 */
    private int retryAttempts = 3;
    /** 连接重试间隔时间（单位：毫秒），默认为1000 */
    private int retryInterval = 1000;
    /** 定期检查连接是否可用的时间间隔（单位：毫秒），默认为0，表示不进行定期检查 */
    private int pingInterval = 0;
    /** 是否保持长连接，默认为true */
    private boolean keepAlive = true;

}
