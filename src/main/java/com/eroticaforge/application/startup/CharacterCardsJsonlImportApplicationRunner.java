package com.eroticaforge.application.startup;

import com.eroticaforge.application.service.CharacterCardsJsonlImporter;
import com.eroticaforge.config.CharacterCardImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 当 {@code erotica.character-cards-import.enable=true} 时，启动后执行一次人物卡 JSONL 导入。
 *
 * @author EroticaForge
 */
@Component
@ConditionalOnProperty(prefix = "erotica.character-cards-import", name = "enable", havingValue = "true")
public class CharacterCardsJsonlImportApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CharacterCardsJsonlImportApplicationRunner.class);

    private final CharacterCardsJsonlImporter importer;
    private final CharacterCardImportProperties properties;

    public CharacterCardsJsonlImportApplicationRunner(
            CharacterCardsJsonlImporter importer, CharacterCardImportProperties properties) {
        this.importer = importer;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String pathStr = properties.getJsonlPath().trim();
        if (pathStr.isEmpty()) {
            throw new IllegalStateException(
                    "erotica.character-cards-import.enable=true requires non-blank jsonl-path "
                            + "(e.g. CHARACTER_CARDS_JSONL).");
        }
        Path jsonlPath = Path.of(pathStr).normalize().toAbsolutePath();
        log.info("Character cards JSONL import (startup): jsonl={}", jsonlPath);
        CharacterCardsJsonlImporter.ImportResult r = importer.importFromJsonl(jsonlPath);
        log.info(
                "Character cards JSONL import done: inserted={}, updated={}, skipped={}, failed={}",
                r.getInserted(),
                r.getUpdated(),
                r.getSkipped(),
                r.getFailed());
    }
}
