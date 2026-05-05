package com.ldq.hragent.service;

import com.ldq.hragent.config.HrKnowledgeProperties;
import com.ldq.hragent.rag.HrDocumentLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HrKnowledgeInitializer {

    private final HrDocumentLoader hrDocumentLoader;
    private final VectorStore pgVectorVectorStore;
    private final HrKnowledgeProperties hrKnowledgeProperties;

    public HrKnowledgeInitializer(
            HrDocumentLoader hrDocumentLoader,
            @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
            HrKnowledgeProperties hrKnowledgeProperties
    ) {
        this.hrDocumentLoader = hrDocumentLoader;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.hrKnowledgeProperties = hrKnowledgeProperties;
    }

    @PostConstruct
    public void init() {
        if (!hrKnowledgeProperties.isAutoInit()) {
            log.info("HR knowledge auto init disabled");
            return;
        }

        if (hrKnowledgeProperties.isRebuildOnStartup()) {
            rebuildKnowledgeBase();
            return;
        }

        long count = countKnowledgeDocuments();
        if (count > 0) {
            log.info("HR knowledge already initialized, count={}", count);
            return;
        }

        loadKnowledgeBase();
    }

    public void loadKnowledgeBase() {
        List<Document> documents = hrDocumentLoader.loadMarkdowns();
        if (documents == null || documents.isEmpty()) {
            log.warn("No HR markdown documents found, skip vector initialization");
            return;
        }

        pgVectorVectorStore.add(documents);
        log.info("HR knowledge initialized successfully, document size={}", documents.size());
    }

    public void rebuildKnowledgeBase() {
        log.info("Start rebuilding HR knowledge base");
        clearKnowledgeBase();
        loadKnowledgeBase();
        log.info("HR knowledge rebuild finished");
    }

    public void clearKnowledgeBase() {
        // 当前 Spring AI 的 VectorStore 接口没有统一清空能力
        // 所以这里后续通过 pgVectorJdbcTemplate 直接 truncate
        throw new UnsupportedOperationException("请调用 HrKnowledgeAdminService.clearKnowledgeBase()");
    }

    public long countKnowledgeDocuments() {
        // 这里只做近似统计，由 admin service 提供真实统计
        return 0L;
    }
}