package com.eroticaforge.application.startup;

import com.eroticaforge.application.service.CorpusJsonlReferenceImporter;
import com.eroticaforge.config.CorpusImportProperties;
import com.eroticaforge.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 当 {@code erotica.corpus-import.enable=true} 时，在应用启动后执行一次 {@link CorpusJsonlReferenceImporter#importFromJsonl}。
 *
 * <p>路径由 {@link CorpusImportProperties} 提供；参考库 {@code story_id} 使用 {@link RagProperties#getReferenceCorpusStoryId()}。
 *
 * @author EroticaForge
 */
@Component
@ConditionalOnProperty(prefix = "erotica.corpus-import", name = "enable", havingValue = "true")
public class CorpusJsonlImportApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusJsonlImportApplicationRunner.class);

    private final CorpusJsonlReferenceImporter importer;
    private final CorpusImportProperties importProperties;
    private final RagProperties ragProperties;

    public CorpusJsonlImportApplicationRunner(
            CorpusJsonlReferenceImporter importer,
            CorpusImportProperties importProperties,
            RagProperties ragProperties) {
        this.importer = importer;
        this.importProperties = importProperties;
        this.ragProperties = ragProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String jsonl = importProperties.getJsonlPath().trim();
        String root = importProperties.getCorpusRoot().trim();
        if (jsonl.isEmpty() || root.isEmpty()) {
            throw new IllegalStateException(
                    "erotica.corpus-import.enable=true requires non-blank jsonl-path and corpus-root "
                            + "(e.g. CORPUS_IMPORT_JSONL and CORPUS_IMPORT_ROOT).");
        }
        Path jsonlPath = Path.of(jsonl).normalize().toAbsolutePath();
        Path corpusRoot = Path.of(root).normalize().toAbsolutePath();
        String referenceStoryId = ragProperties.getReferenceCorpusStoryId();
        log.info(
                "Corpus JSONL import (startup): jsonl={}, corpusRoot={}, referenceStoryId={}",
                jsonlPath,
                corpusRoot,
                referenceStoryId);
        CorpusJsonlReferenceImporter.ImportResult result =
                importer.importFromJsonl(jsonlPath, corpusRoot, referenceStoryId);
        log.info(
                "Corpus JSONL import done: imported={}, skipped={}, failed={}",
                result.getImported(),
                result.getSkipped(),
                result.getFailed());
        if (result.getFailed() > 0) {
            log.warn(
                    "Corpus JSONL import completed with failed>0; check logs above. "
                            + "Set erotica.rag.include-reference-corpus=true after verifying data to use reference RAG.");
        }
    }
}
