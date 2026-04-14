package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.rag.HrDocumentLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HrKnowledgeAdminService {

    private final JdbcTemplate pgVectorJdbcTemplate;
    private final VectorStore pgVectorVectorStore;
    private final HrDocumentLoader hrDocumentLoader;
    private final PermissionService permissionService;

    public HrKnowledgeAdminService(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
            @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
            HrDocumentLoader hrDocumentLoader,
            PermissionService permissionService
    ) {
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.hrDocumentLoader = hrDocumentLoader;
        this.permissionService = permissionService;
    }
    @com.yupi.yuaiagent.aop.AuditLog(operationName = "重建知识库", operationModule = "knowledge")
    public String rebuildKnowledgeBase() {
        permissionService.requireAdmin();
        clearKnowledgeBase();

        List<Document> documents = hrDocumentLoader.loadMarkdowns();
        if (documents == null || documents.isEmpty()) {
            return "未发现可导入的知识文档，重建已终止。";
        }

        pgVectorVectorStore.add(documents);
        return "知识库重建完成，共导入 " + documents.size() + " 个文档片段。";
    }
    @com.yupi.yuaiagent.aop.AuditLog(operationName = "清空知识库", operationModule = "knowledge")
    public String clearKnowledgeBase() {
        permissionService.requireAdmin();

        pgVectorJdbcTemplate.execute("TRUNCATE TABLE hr_knowledge_vector RESTART IDENTITY");
        return "知识库已清空。";
    }

    public Integer countKnowledgeRows() {
        permissionService.requireAdmin();
        Integer count = pgVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM hr_knowledge_vector",
                Integer.class
        );
        return count == null ? 0 : count;
    }
}