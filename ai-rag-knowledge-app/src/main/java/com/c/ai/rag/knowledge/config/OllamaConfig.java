package com.c.ai.rag.knowledge.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

/**
 * Ollama 配置类
 * 负责配置 AI 相关的核心组件，包括：
 * 1. Ollama API 客户端
 * 2. ChatClient 实例
 * 3. OllamaEmbeddingModel 嵌入模型
 * 4. PgVectorStore 向量存储
 * 5. TokenTextSplitter 文本分割器
 * 6. ObservationRegistry 观测注册表
 *
 * @author cyh
 * @date 2026/04/02
 */
@Configuration
public class OllamaConfig {

    /** Ollama API 基础 URL */
    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    /** 嵌入模型名称 */
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String modelName;

    // --- 1. AI 大脑 (OllamaEmbeddingModel) ---

    /**
     * 创建 Ollama API 客户端实例
     * 用于与 Ollama 服务进行通信，提供模型调用能力
     *
     * @return OllamaApi 实例
     */
    @Bean
    public OllamaApi ollamaApi() {
        // 使用 Builder 模式创建 OllamaApi 实例，设置基础 URL
        return OllamaApi
                .builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 创建 ChatClient 实例
     * 用于与 AI 模型进行对话交互
     *
     * @param builder ChatClient 构建器
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // 你可以在这里配置默认的 Advisor（如聊天记忆）或默认设置
        return builder.build();
    }

    /**
     * 创建 OllamaEmbeddingModel 实例
     * 用于生成文本的向量嵌入，是 AI 系统的"大脑"
     *
     * @param ollamaApi           Ollama API 客户端
     * @param observationRegistry 观测注册表
     * @return OllamaEmbeddingModel 实例
     */
    @Bean
    public OllamaEmbeddingModel embeddingModel(OllamaApi ollamaApi, ObservationRegistry observationRegistry) {
        // 配置嵌入模型选项
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions
                .builder()
                .model(modelName)
                .build();

        // 配置模型管理选项
        ModelManagementOptions managementOptions = ModelManagementOptions
                .builder()
                .pullModelStrategy(PullModelStrategy.WHEN_MISSING) // 当模型缺失时自动拉取
                .timeout(Duration.ofMinutes(5)) // 超时时间设置为 5 分钟
                .build();

        // 遵循最新版 Builder 模式创建嵌入模型实例
        return OllamaEmbeddingModel
                .builder()
                .ollamaApi(ollamaApi) // 设置 Ollama API 客户端
                .defaultOptions(options) // 设置默认选项
                .observationRegistry(observationRegistry) // 设置观测注册表
                .modelManagementOptions(managementOptions) // 设置模型管理选项
                .build();
    }

    // --- 2. 存储身体 (PgVectorStore) ---

    /**
     * 创建 PgVectorStore 实例
     * 用于存储和检索向量嵌入，是 AI 系统的"存储身体"
     *
     * @param jdbcTemplate   JDBC 模板，用于数据库操作
     * @param embeddingModel 嵌入模型，用于生成向量
     * @return PgVectorStore 实例
     */
    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, OllamaEmbeddingModel embeddingModel) {
        return PgVectorStore
                .builder(jdbcTemplate, embeddingModel)
                .dimensions(768)                           // nomic-embed-text 模型的向量维度
                .distanceType(PgDistanceType.COSINE_DISTANCE) // 使用余弦距离计算相似度
                .indexType(PgIndexType.HNSW)               // 使用 HNSW 索引提高搜索速度
                .initializeSchema(true)                    // 自动建表和扩展
                .build();
    }

    // --- 3. 辅助挂件 ---

    /**
     * 创建 TokenTextSplitter 实例
     * 用于将文本分割成适合模型处理的小块
     *
     * @return TokenTextSplitter 实例
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        // 创建默认的 TokenTextSplitter 实例
        return new TokenTextSplitter();
    }

    /**
     * 创建 ObservationRegistry 实例
     * 用于观测和监控系统运行状态
     *
     * @return ObservationRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ObservationRegistry observationRegistry() {
        // 创建新的 ObservationRegistry 实例
        return ObservationRegistry.create();
    }
}