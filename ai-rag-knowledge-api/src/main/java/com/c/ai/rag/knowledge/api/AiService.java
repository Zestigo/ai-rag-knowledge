package com.c.ai.rag.knowledge.api;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 服务接口，定义了与 AI 模型交互的核心方法
 * 提供了同步和流式两种生成方式，支持指定模型和消息内容
 *
 * @author cyh
 * @date 2026/04/01
 */
public interface AiService {
    /**
     * 同步生成 AI 响应
     * 调用指定的 AI 模型，根据输入消息生成响应
     *
     * @param model   模型名称
     * @param message 输入消息内容
     * @return ChatResponse AI 模型的响应结果
     */
    ChatResponse generate(String model, String message);

    /**
     * 流式生成 AI 响应
     * 调用指定的 AI 模型，以流式方式返回响应结果
     *
     * @param model   模型名称
     * @param message 输入消息内容
     * @return Flux<ChatResponse> 流式的 AI 模型响应结果
     */
    Flux<ChatResponse> generateStream(String model, String message);
}
