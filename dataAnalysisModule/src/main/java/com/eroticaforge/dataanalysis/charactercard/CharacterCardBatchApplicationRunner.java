package com.eroticaforge.dataanalysis.charactercard;

import com.eroticaforge.dataanalysis.config.CharacterCardProcessingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 可选：启动时跑人物卡批量提取（profile 或 {@code erotica.character-card.batch.enabled}）。
 *
 * @author EroticaForge
 */
@Component
@ConditionalOnProperty(prefix = "erotica.character-card.batch", name = "enabled", havingValue = "true")
public class CharacterCardBatchApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CharacterCardBatchApplicationRunner.class);

    private final CharacterCardExtractorService extractorService;
    private final CharacterCardProcessingProperties properties;

    public CharacterCardBatchApplicationRunner(
            CharacterCardExtractorService extractorService,
            CharacterCardProcessingProperties properties) {
        this.extractorService = extractorService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CharacterCardProcessingProperties.Batch batch = properties.getBatch();
        String in = batch.getInputDirectory().trim();
        String out = batch.getOutputJsonl().trim();
        if (in.isEmpty() || out.isEmpty()) {
            log.warn("character-card.batch.enabled=true 但未配置 input-directory 或 output-jsonl，跳过");
            return;
        }
        Path outPath = Path.of(out).normalize();
        Path skipped = properties.resolveSkippedJsonlPath(outPath);
        log.info(
                "character card batch start: input={}, output={}, skipped={}, resume={}",
                Path.of(in).normalize(),
                outPath,
                skipped != null ? skipped.normalize() : "(disabled)",
                properties.isResumeEnabled());
        extractorService.extractDirectoryToJsonl(Path.of(in).normalize(), outPath);
    }
}
