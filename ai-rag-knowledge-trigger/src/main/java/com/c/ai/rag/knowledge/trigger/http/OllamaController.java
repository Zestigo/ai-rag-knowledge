package com.c.ai.rag.knowledge.trigger.http;

import com.c.ai.rag.knowledge.api.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Ollama 控制器，实现 AiService 接口
 * 提供 AI 聊天和流式聊天的 HTTP 接口
 *
 * @author cyh
 * @date 2026/04/01
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1/ollama")
public class OllamaController implements AiService {

    /** Ollama 聊天模型 */
    private final OllamaChatModel chatModel;

    /**
     * 构造方法，注入 OllamaChatModel
     *
     * @param chatModel Ollama 聊天模型实例
     */
    @Autowired
    public OllamaController(OllamaChatModel chatModel) {
        this.chatModel = chatModel; // 保存注入的聊天模型实例
        log.info("OllamaController 初始化完成"); // 记录控制器初始化完成日志
    }

    /**
     * 同步生成 AI 响应
     * 示例请求: <a href="http://localhost:8090/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=hello">...</a>
     *
     * @param model   模型名称
     * @param message 输入消息内容
     * @return ChatResponse AI 模型的响应结果
     */
    @GetMapping("generate")
    @Override
    public ChatResponse generate(@RequestParam("model") String model, @RequestParam("message") String message) {
        log.info("接收到同步生成请求，模型: {}, 消息: {}", model, message);
        // 使用 OllamaChatOptions 构建局部配置，覆盖默认配置
        OllamaChatOptions ollamaChatOptions = OllamaChatOptions
                .builder()
                .model(model) // 设置请求的模型名称
                .build(); // 构建配置对象

        // 构造 Prompt，将局部 ollamaChatOptions 传入，这会覆盖 Bean 里的默认配置
        Prompt prompt = new Prompt(message, ollamaChatOptions); // 创建包含消息和配置的提示对象

        ChatResponse response = chatModel.call(prompt); // 调用聊天模型生成响应
        log.info("同步生成响应完成");
        return response; // 返回 AI 模型的响应结果
    }

    /**
     * 流式生成 AI 响应
     * 示例请求: <a href="http://localhost:8090/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=hello">...</a>
     *
     * @param model   模型名称
     * @param message 输入消息内容
     * @return Flux<ChatResponse> 流式的 AI 模型响应结果
     */
    @GetMapping("generate_stream")
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam("model") String model,
                                             @RequestParam("message") String message) {
        log.info("接收到流式生成请求，模型: {}, 消息: {}", model, message);
        // 构建局部配置，覆盖默认配置
        OllamaChatOptions ollamaChatOptions = OllamaChatOptions
                .builder()
                .model(model) // 设置请求的模型名称
                .build(); // 构建配置对象

        // 创建包含消息和配置的提示对象
        Prompt prompt = new Prompt(message, ollamaChatOptions);
        // 调用聊天模型生成流式响应
        Flux<ChatResponse> response = chatModel.stream(prompt);
        log.info("流式生成响应开始");
        return response; // 返回流式的 AI 模型响应结果
    }
}