package com.eroticaforge;

import com.eroticaforge.config.CorpusImportProperties;
import com.eroticaforge.config.GenerationProperties;
import com.eroticaforge.config.PromptProperties;
import com.eroticaforge.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * EroticaForge 后端入口。
 *
 * @author EroticaForge
 */
@SpringBootApplication
@EnableConfigurationProperties({
    RagProperties.class,
    GenerationProperties.class,
    PromptProperties.class,
    CorpusImportProperties.class
})
public class EroticaForgeApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数（由 Spring Boot 解析）
     */
    public static void main(String[] args) {
        SpringApplication.run(EroticaForgeApplication.class, args);
    }
}
