package com.c.ai.rag.knowledge;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 测试类
 * 用于测试 RAG (Retrieval-Augmented Generation) 相关功能，包括：
 * 1. 文档上传和向量存储
 * 2. 基于向量搜索的问答
 *
 * @author cyh
 * @date 2026/04/02
 */
@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.NONE) // 测试环境不需要 Web 容器
public class RAGTest {

    /** ChatClient 实例，用于与 AI 模型进行对话 */
    @Resource
    private ChatClient chatClient;

    /** TokenTextSplitter 实例，用于将文本分割成小块 */
    @Resource
    private TokenTextSplitter tokenTextSplitter;

    /** PgVectorStore 实例，用于存储和检索向量嵌入 */
    @Resource
    private PgVectorStore pgVectorStore;

    /**
     * 测试文档上传功能
     * 步骤：
     * 1. 读取文档文件
     * 2. 对文档进行文本切片
     * 3. 为每个切片添加元数据
     * 4. 将切片存入向量存储
     */
    @Test
    public void upload() {
        // 1. 读取文档
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.text");
        List<Document> documents = reader.get();

        // 2. 文本切片
        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        // 3. 为每个切片注入元数据（用于后续 Filter 过滤）
        documentSplitterList.forEach(doc -> doc
                .getMetadata()
                .put("knowledge", "知识库名称"));

        // 4. 存入 PgVectorStore (身体 + 血液)
        pgVectorStore.accept(documentSplitterList);

        log.info("上传完成，切片数量：{}", documentSplitterList.size());
    }

    /**
     * 测试基于 RAG 的聊天功能
     * 步骤：
     * 1. 构建搜索请求
     * 2. 执行向量搜索获取相关文档
     * 3. 提取文档内容作为上下文
     * 4. 使用 ChatClient 发起对话并获取回答
     */
    @Test
    public void chat() {
        String userQuery = "王大瓜，哪年出生";

        // 1. 构建搜索请求（参考 SearchRequest 源码）
        // 注意：根据源码，静态方法 builder() 是创建请求的最佳方式
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(userQuery) // 设置查询文本
                .topK(5) // 获取前 5 个最相关的片段
                .similarityThreshold(0.7) // 可选：设置相似度阈值
                .filterExpression("knowledge == '知识库名称'") // 元数据过滤，只搜索特定知识库的内容
                .build();

        // 2. 执行向量搜索
        List<Document> similarDocuments = pgVectorStore.similaritySearch(searchRequest);

        // 3. 提取文档内容，将所有相关文档的文本连接成一个字符串
        String context = similarDocuments
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 4. 使用 ChatClient 的 Fluent API 发起对话（参考 ChatClientRequestSpec 源码）
        String response = chatClient.prompt() // 创建提示
                                    .system(sp -> sp // 设置系统提示
                                                     .text("""
                                                             Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                                                             If unsure, simply state that you don't know.
                                                             Your reply must be in Chinese!
                                                             
                                                             DOCUMENTS:
                                                             {documents}
                                                             """)
                                                     .param("documents", context)) // 动态注入搜索到的文档作为上下文
                                    .user(userQuery) // 设置用户查询
                                    .options(OllamaChatOptions // 设置模型选项
                                                               .builder()
                                                               .model("deepseek-r1:1.5b") // 使用指定的模型
                                                               .build())
                                    .call() // 发起调用
                                    .content(); // 直接获取字符串内容

        log.info("AI 回答结果：{}", response);
    }
}