package com.c.ai.rag.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI RAG Knowledge 应用主类
 * 描述: Spring Boot 应用的入口点，负责启动整个应用
 * 版本: 1.0
 * 日期: 2026-04-01
 */
@SpringBootApplication
public class Application {

    /**
     * 应用主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(Application.class, args);
    }

}
