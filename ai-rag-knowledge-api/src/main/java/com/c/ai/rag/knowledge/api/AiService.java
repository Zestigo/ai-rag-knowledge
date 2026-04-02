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
     * 上传文档至知识库
     * 将文件解析、切片并向量化存储至指定的知识库中
     *
     * @param file          待上传的 MultipartFile 文件
     * @param knowledgeName 知识库分类标识
     * @return 存储结果描述及切片统计
     * @throws IOException 文件处理过程中的 IO 异常
     */
    String uploadDocument(MultipartFile file, String knowledgeName) throws IOException;

    /**
     * RAG 检索增强对话（流式）
     * 结合向量数据库召回上下文，并引导模型基于特定知识库内容回答问题
     *
     * @param message       用户的查询文本
     * @param knowledgeName 检索的目标知识库名称
     * @return 实时生成的增量字符串文本流
     */
    Flux<String> ragChat(String message, String knowledgeName);

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