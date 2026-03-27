package com.eroticaforge.dataanalysis;

import com.eroticaforge.dataanalysis.config.CharacterCardProcessingProperties;
import com.eroticaforge.dataanalysis.config.CorpusProcessingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 语料数据分析独立应用入口（与主后端 {@code eroticaforge} 分离）。
 *
 * @author EroticaForge
 */
@SpringBootApplication
@EnableConfigurationProperties({CorpusProcessingProperties.class, CharacterCardProcessingProperties.class})
public class DataAnalysisApplication {

    /**
     * 启动应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DataAnalysisApplication.class, args);
    }
}
