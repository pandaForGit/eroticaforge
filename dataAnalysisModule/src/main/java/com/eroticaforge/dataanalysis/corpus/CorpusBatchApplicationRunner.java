package com.eroticaforge.dataanalysis.corpus;

import com.eroticaforge.dataanalysis.config.CorpusProcessingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 可选：开启后于应用启动时跑一轮语料分类（适合本机长时间任务）。
 *
 * <p>配置见本模块 {@code application.yml} 中 {@code erotica.corpus.batch}。
 *
 * @author EroticaForge
 */
@Component
@ConditionalOnProperty(prefix = "erotica.corpus.batch", name = "enabled", havingValue = "true")
public class CorpusBatchApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusBatchApplicationRunner.class);

    private final NovelClassifierService novelClassifierService;
    private final CorpusProcessingProperties corpusProcessingProperties;

    public CorpusBatchApplicationRunner(
            NovelClassifierService novelClassifierService,
            CorpusProcessingProperties corpusProcessingProperties) {
        this.novelClassifierService = novelClassifierService;
        this.corpusProcessingProperties = corpusProcessingProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CorpusProcessingProperties.Batch batch = corpusProcessingProperties.getBatch();
        String in = batch.getInputDirectory().trim();
        String out = batch.getOutputJsonl().trim();
        if (in.isEmpty() || out.isEmpty()) {
            log.warn("erotica.corpus.batch.enabled=true 但未配置 input-directory 或 output-jsonl，跳过批处理");
            return;
        }
        Path outPath = Path.of(out).normalize();
        Path skippedPath = corpusProcessingProperties.resolveSkippedJsonlPath(outPath);
        Path skippedNorm = skippedPath != null ? skippedPath.normalize() : null;
        log.info(
                "corpus batch start: input={}, output={}, skipped={}, resume={}",
                in,
                outPath,
                skippedNorm != null ? skippedNorm : "(disabled)",
                corpusProcessingProperties.isResumeEnabled());
        novelClassifierService.classifyDirectoryToJsonl(Path.of(in).normalize(), outPath);
    }
}
