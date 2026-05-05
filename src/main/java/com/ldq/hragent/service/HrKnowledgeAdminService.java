package com.ldq.hragent.service;

import com.ldq.hragent.rag.HrDocumentLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @com.ldq.hragent.aop.AuditLog(operationName = "重建知识库", operationModule = "knowledge")
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

    @com.ldq.hragent.aop.AuditLog(operationName = "清空知识库", operationModule = "knowledge")
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

    @com.ldq.hragent.aop.AuditLog(operationName = "上传知识文件", operationModule = "knowledge")
    public String uploadKnowledgeFile(MultipartFile file, String topic, String docType) {
        permissionService.requireAdmin();

        if (file == null || file.isEmpty()) {
            return "上传失败：文件不能为空。";
        }

        String filename = file.getOriginalFilename() == null ? "unknown.txt" : file.getOriginalFilename();
        String lower = filename.toLowerCase();

        if (!(lower.endsWith(".txt") || lower.endsWith(".md"))) {
            return "当前仅支持上传 txt / md 文件。";
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (content.isBlank()) {
                return "上传失败：文件内容为空。";
            }

            List<Document> documents = splitToDocuments(content, filename, topic, docType);
            pgVectorVectorStore.add(documents);

            return "上传成功，共写入 " + documents.size() + " 个知识片段。";
        } catch (Exception e) {
            log.error("uploadKnowledgeFile error", e);
            return "上传失败：" + e.getMessage();
        }
    }

    private List<Document> splitToDocuments(String content,
                                            String filename,
                                            String topic,
                                            String docType) {
        List<Document> documents = new ArrayList<>();

        int chunkSize = 500;
        int overlap = 80;
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunk = content.substring(start, end);

            documents.add(new Document(
                    chunk,
                    Map.of(
                            "filename", filename,
                            "topic", safe(topic, "custom_upload"),
                            "docType", safe(docType, "uploaded_text")
                    )
            ));

            if (end == content.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }

        return documents;
    }

    private String safe(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}