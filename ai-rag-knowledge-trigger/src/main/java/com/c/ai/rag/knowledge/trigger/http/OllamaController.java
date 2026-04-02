package com.c.ai.rag.knowledge.trigger.http;

import com.c.ai.rag.knowledge.api.AiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
@RequestMapping("api/v1/ollama")
@AllArgsConstructor
public class OllamaController implements AiService {
    /** Ollama 聊天模型执行引擎 */
    private final OllamaChatModel chatModel;
    /** Spring AI 统一对话客户端 */
    private final ChatClient chatClient;
    /** PostgreSQL 向量存储库 */
    private final PgVectorStore pgVectorStore;
    /** 文本分词切片器 */
    private final TokenTextSplitter tokenTextSplitter;

    /**
     * 上传本地文档并持久化至向量知识库
     * 支持多种格式文档解析，自动分段并关联知识库标识
     *
     * @param file          待上传的多部分文件
     * @param knowledgeName 关联的知识库分类名称
     * @return 状态提示信息及切片统计
     * @throws IOException 文件读写或转换异常
     */
    @Override
    @PostMapping("upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam("knowledge") String knowledgeName) throws IOException {
        // 构建临时存储路径，确保跨平台兼容性
        String tempPath = Paths
                .get(System.getProperty("java.io.tmpdir"), file.getOriginalFilename())
                .toString();
        File tempFile = new File(tempPath);
        // 将内存中的 MultipartFile 转移到本地临时磁盘文件
        file.transferTo(tempFile);
        try {
            // 使用 Tika 引擎自动识别并读取多种格式的内容
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(tempFile));
            List<Document> documents = reader.get();
            // 将长文档按 Token 限制拆分为更小的切片，以便向量检索
            List<Document> splitDocs = tokenTextSplitter.apply(documents);
            // 遍历每个文档切片，在元数据中注入知识库名称，方便后续分组过滤
            splitDocs.forEach(doc -> doc
                    .getMetadata()
                    .put("knowledge", knowledgeName));
            // 将切片后的文档向量化并存入 PostgreSQL 向量表
            pgVectorStore.accept(splitDocs);
            return "文件上传并存入知识库成功，切片数：" + splitDocs.size();
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("无法删除临时文件: {}", tempFile.getAbsolutePath());
                    // 备选方案：程序退出时尝试删除
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    /**
     * RAG 模式下的检索增强流式对话
     * 结合向量数据库召回背景知识，并引导模型基于上下文回答
     *
     * @param message       用户的具体问题
     * @param knowledgeName 需要检索的特定知识库
     * @return 实时生成的字符串内容流
     */
    @Override
    @GetMapping("rag_chat")
    public Flux<String> ragChat(@RequestParam("message") String message,
                                @RequestParam("knowledge") String knowledgeName) {
        // 构建向量搜索请求：包含相似度阈值、返回数量及基于元数据的元数据过滤
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(message)
                .topK(3)
                .similarityThreshold(0.7)
                .filterExpression("knowledge == '" + knowledgeName + "'")
                .build();
        // 从向量库中检索语义最相关的文档片段
        List<Document> similarDocs = pgVectorStore.similaritySearch(searchRequest);
        // 将检索到的多个文档片段文本合并成一个大的上下文字符串
        String context = similarDocs
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        // 使用 ChatClient 链式 API 构建 Prompt，注入 System 预设与 Context 变量
        return chatClient
                .prompt()
                .system(s -> s
                        .text("""
                                你是一个专业的知识库助手。请根据以下提供的文档内容回答问题。
                                如果你不确定，请回答不知道。必须使用中文回复。
                                
                                DOCUMENTS:
                                {documents}
                                """)
                        .param("documents", context))
                .user(message)
                .stream()
                .content(); // 仅向下游订阅者推送生成的纯文本增量片段
    }

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