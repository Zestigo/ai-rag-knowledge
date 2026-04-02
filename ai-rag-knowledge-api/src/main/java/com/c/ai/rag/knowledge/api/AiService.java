package com.c.ai.rag.knowledge.api;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * AI 服务核心契约接口
 * 定义了文档上传、基础对话及 RAG（检索增强生成）的标准行为，支持同步与响应式流式输出
 *
 * @author cyh
 * @date 2026/04/02
 */
public interface AiService {

    /**
     * 同步生成 AI 响应（指定模型）
     * 指定特定的 AI 模型执行对话任务并获取完整结果
     *
     * @param model   模型标识符，如 "deepseek-r1"
     * @param message 用户输入的提示词文本
     * @return 包含生成内容及元数据的 ChatResponse 对象
     */
    ChatResponse generate(String model, String message);

    /**
     * 响应式流式生成 AI 响应（指定模型）
     * 适用于长文本实时输出，提供 ChatResponse 对象的增量响应流
     *
     * @param model   模型标识符
     * @param message 用户输入的提示词文本
     * @return 响应式 ChatResponse 异步流
     */
    Flux<ChatResponse> generateStream(String model, String message);

}