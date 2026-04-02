package com.c.ai.rag.knowledge.trigger.http;

import com.c.ai.rag.knowledge.api.AiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Ollama 接口控制器
 * 集成了 Spring AI Ollama 客户端与 PgVector 向量数据库
 * 实现基础的 AI 聊天对话及 RAG (检索增强生成) 知识库上传与检索功能
 *
 * @author cyh
 * @date 2026/04/02
 */
@Slf4j
@RestController
@CrossOrigin("*")
@AllArgsConstructor
@RequestMapping("api/v1/ollama")
public class OllamaController implements AiService {

    /** Ollama 聊天模型执行引擎 */
    private final OllamaChatModel chatModel;

    /**
     * 同步生成 AI 响应
     * 直接通过模型实例指定模型名称并获取完整回复
     *
     * @param model   使用的 AI 模型标识
     * @param message 用户的查询文本
     * @return 包含元数据的完整 ChatResponse 对象
     */
    @Override
    @GetMapping("generate")
    public ChatResponse generate(@RequestParam("model") String model, @RequestParam("message") String message) {
        log.info("接收到同步生成请求，模型: {}, 消息: {}", model, message);
        // 动态构建 Ollama 配置，通过 builder 模式覆盖默认的模型参数
        OllamaChatOptions ollamaChatOptions = OllamaChatOptions
                .builder()
                .model(model)
                .build();
        // 包装请求消息与运行期参数
        Prompt prompt = new Prompt(message, ollamaChatOptions);
        // 执行阻塞式调用直到模型生成全部内容
        ChatResponse response = chatModel.call(prompt);
        log.info("同步生成响应完成");
        return response;
    }

    /**
     * 流式生成 AI 响应
     * 以响应式流的方式实时返回模型生成的 Token
     *
     * @param model   使用的 AI 模型标识
     * @param message 用户的查询文本
     * @return 包含部分响应结果的 ChatResponse 响应流
     */
    @Override
    @GetMapping("generate_stream")
    public Flux<ChatResponse> generateStream(@RequestParam("model") String model,
                                             @RequestParam("message") String message) {
        log.info("接收到流式生成请求，模型: {}, 消息: {}", model, message);
        // 配置运行期模型参数
        OllamaChatOptions ollamaChatOptions = OllamaChatOptions
                .builder()
                .model(model)
                .build();
        // 初始化 Prompt 请求体
        Prompt prompt = new Prompt(message, ollamaChatOptions);
        // 调用流式接口，返回响应式 Flux 序列
        Flux<ChatResponse> response = chatModel.stream(prompt);
        log.info("流式生成响应开始");
        return response;
    }
}