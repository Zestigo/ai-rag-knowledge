package com.c.ai.rag.knowledge.trigger.http;

import com.c.ai.rag.knowledge.api.RAGService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 业务控制器
 * 封装了基于 Spring AI 的文档向量化流程与检索增强对话逻辑
 * 集成了 Redisson 实现标签管理，通过 PgVector 进行向量存储
 *
 * @author cyh
 * @date 2026/04/02
 */
@Slf4j
@RestController
@CrossOrigin("*")
@AllArgsConstructor
@RequestMapping("api/v1/rag")
public class RAGController implements RAGService {

    /** Spring AI 统一对话客户端 */
    private final ChatClient chatClient;
    /** PostgreSQL 向量存储库 */
    private final PgVectorStore pgVectorStore;
    /** 文本分词切片器 */
    private final TokenTextSplitter tokenTextSplitter;
    /** Redis 客户端，用于维护知识库标签列表 */
    private final RedissonClient redissonClient;

    /**
     * 批量上传文档并同步标签至 Redis
     * 该方法实现了文件的物理落地、文本解析、向量化存储及标签去重记录
     *
     * @param files         前端上传的文件集合
     * @param knowledgeName 归属的知识库分类
     * @return 处理成功的统计信息
     * @throws IOException 文件流转存异常
     */
    @Override
    @PostMapping("upload")
    public String uploadDocuments(@RequestParam("files") List<MultipartFile> files,
                                  @RequestParam("knowledge") String knowledgeName) throws IOException {
        log.info("开始处理批量上传，知识库标签: {}, 文件数量: {}", knowledgeName, files.size());
        int totalChunks = 0;
        // 循环处理每一个上传的文件
        for (MultipartFile file : files) {
            // 1. 落地磁盘临时文件：防止大文件直接进入内存导致 OOM，确保 Tika 可以通过资源路径读取
            String tempPath = Paths
                    .get(System.getProperty("java.io.tmpdir"),
                            System.currentTimeMillis() + "_" + file.getOriginalFilename())
                    .toString();
            File tempFile = new File(tempPath);
            // 将内存流转存到磁盘临时目录
            file.transferTo(tempFile);
            try {
                // 2. 解析与切片：利用 Tika 自动识别文件格式提取文本，并按 Token 长度拆分
                TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(tempFile));
                List<Document> documents = reader.get();
                List<Document> splitDocs = tokenTextSplitter.apply(documents);
                // 3. 注入元数据：为每一段切片标记所属知识库，以便后续检索时进行过滤
                splitDocs.forEach(doc -> doc
                        .getMetadata()
                        .put("knowledge", knowledgeName));
                // 4. 存入向量库：通过 Embedding 模型将文本转为向量并存入数据库
                pgVectorStore.accept(splitDocs);
                totalChunks += splitDocs.size();
            } finally {
                // 执行清理工作
                if (tempFile.exists()) {
                    try {
                        // 尝试立即删除临时磁盘文件
                        if (!tempFile.delete()) {
                            log.warn("立即删除失败，已标记为退出时删除: {}", tempFile.getName());
                            // 若因句柄未释放等原因失败，则注册钩子在 JVM 退出时尝试清理
                            tempFile.deleteOnExit();
                        }
                    } catch (SecurityException e) {
                        log.error("权限不足，无法删除文件: {}", e.getMessage());
                    }
                }
            }
        }
        // 6. 将知识库名称同步至 Redis 列表：供前端筛选展示
        RList<String> tags = redissonClient.getList("ragTag");
        if (!tags.contains(knowledgeName)) {
            tags.add(knowledgeName);
        }
        return "批量上传成功，共处理文件: " + files.size() + "，总切片数: " + totalChunks;
    }

    /**
     * 查询所有已存在的知识库标签列表
     * 从 Redis 中获取去重后的标签集合
     *
     * @return 标签字符串列表
     */
    @GetMapping("tags")
    public List<String> queryRagTagList() {
        RList<String> tags = redissonClient.getList("ragTag");
        return new ArrayList<>(tags);
    }

    /**
     * 检索增强流式对话
     * 逻辑：先搜索向量库 -> 提取背景知识 -> 注入提示词 -> 流式生成回答
     *
     * @param message       用户提问的内容
     * @param knowledgeName 限定检索的知识库范围
     * @return 字符流响应
     */
    @Override
    @GetMapping("chat")
    public Flux<String> ragChat(@RequestParam("message") String message,
                                @RequestParam("knowledge") String knowledgeName) {
        log.info("RAG 检索请求: {}, 目标知识库: {}", message, knowledgeName);
        // 构建向量搜索请求：设定 Top 3 相似度及知识库过滤条件
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(message)
                .topK(3)
                .similarityThreshold(0.7)
                .filterExpression("knowledge == '" + knowledgeName + "'")
                .build();
        // 执行相似度检索获取最相关的文档片段
        List<Document> similarDocs = pgVectorStore.similaritySearch(searchRequest);
        // 将检索到的文本片段拼接成统一的上下文背景
        String context = similarDocs
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        // 构造提示词并调用 ChatClient 获取流式输出内容
        return chatClient
                .prompt()
                .system(s -> s
                        .text("你是一个知识库助手。请根据提供的文档回答问题。不知道请直说。")
                        .param("documents", context))
                .user(message)
                .stream()
                .content();
    }
}