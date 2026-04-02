package com.c.ai.rag.knowledge.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * RAG 服务核心契约接口
 * 定义了文档知识库的批量上传、持久化逻辑以及基于检索增强的流式对话行为
 *
 * @author cyh
 * @date 2026/04/02
 */
public interface RAGService {

    /**
     * 查询所有已存在的知识库标签列表
     * 从 Redis 中获取去重后的标签集合
     *
     * @return 标签字符串列表
     */
    List<String> queryRagTagList();

    /**
     * 批量上传文档并解析存入向量知识库
     * 支持多种格式文件，自动执行文本提取、分片、向量化及标签关联
     *
     * @param files         待处理的多部分文件列表
     * @param knowledgeName 知识库分类标签名称
     * @return 包含处理结果详情及切片总数的提示字符串
     * @throws IOException 当文件读取、临时存储或转换过程中发生 IO 错误时抛出
     */
    String uploadDocuments(List<MultipartFile> files, String knowledgeName) throws IOException;

    /**
     * 执行检索增强生成（RAG）流式对话
     * 根据用户问题从指定知识库检索相关上下文，并引导模型生成回答
     *
     * @param message       用户的查询文本
     * @param knowledgeName 检索的目标知识库标签
     * @return 响应式的增量文本字符串流
     */
    Flux<String> ragChat(String message, String knowledgeName);
}