package com.c.ai.rag.knowledge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类，负责初始化和配置 AI 模型相关的 Bean
 * 包括 Ollama 聊天模型和嵌入模型的配置
 *
 * @author cyh
 * @date 2026/04/01
 */
@Slf4j
@Configuration
public class AiConfig {

    /** Ollama API 基础 URL */
    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    /** 聊天模型名称 */
    @Value("${spring.ai.ollama.chat.options.model}")
    private String chatModel;

    /** 嵌入模型名称 */
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String embeddingModel;

    /**
     * 创建并配置 Ollama 聊天模型 Bean
     * 用于处理 AI 聊天请求
     *
     * @return OllamaChatModel 实例
     */
    @Bean
    public OllamaChatModel ollamaChatModel() {
        log.info("正在初始化 Ollama 聊天模型，使用模型: {}", chatModel); // 记录模型初始化信息
        // 构建 Ollama API 客户端
        var ollamaApi = OllamaApi
                .builder()
                .baseUrl(baseUrl) // 设置 Ollama API 基础 URL
                .build();

        // 构建并返回 Ollama 聊天模型
        return OllamaChatModel
                .builder()
                .ollamaApi(ollamaApi) // 设置 API 客户端
                .defaultOptions(OllamaChatOptions // 设置默认配置选项
                                                  .builder()
                                                  .model(chatModel) // 动态读取 YAML 配置的模型名称
                                                  .temperature(0.7) // 设置温度参数，控制生成的随机性
                                                  .build()) // 构建配置选项
                .build(); // 构建聊天模型实例
    }

    /**
     * 创建并配置 Ollama 嵌入模型 Bean
     * 用于生成文本嵌入向量
     *
     * @return OllamaEmbeddingModel 实例
     */
    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel() {
        log.info("正在初始化 Ollama 嵌入模型，使用模型: {}", embeddingModel); // 记录模型初始化信息
        // 构建 Ollama API 客户端
        var ollamaApi = OllamaApi
                .builder()
                .baseUrl(baseUrl) // 设置 Ollama API 基础 URL
                .build(); // 构建 API 客户端实例

        // 构建并返回 Ollama 嵌入模型
        return OllamaEmbeddingModel
                .builder()
                .ollamaApi(ollamaApi) // 设置 API 客户端
                .defaultOptions(OllamaEmbeddingOptions // 设置默认配置选项
                                                       .builder()
                                                       .model(embeddingModel) // 动态读取 YAML 配置的模型名称
                                                       .build()) // 构建配置选项
                .build(); // 构建嵌入模型实例
    }
}