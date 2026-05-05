package com.ldq.hragent.service;

import com.ldq.hragent.config.HrKnowledgeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HrKnowledgeService {

    private final VectorStore pgVectorVectorStore;
    private final HrKnowledgeProperties hrKnowledgeProperties;

    public HrKnowledgeService(
            @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
            HrKnowledgeProperties hrKnowledgeProperties
    ) {
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.hrKnowledgeProperties = hrKnowledgeProperties;
    }

    public List<Document> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .build();

        return pgVectorVectorStore.similaritySearch(request);
    }

    public List<Document> searchByTopic(String query, String topic) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .filterExpression("topic == '" + safeValue(topic) + "'")
                .build();

        return pgVectorVectorStore.similaritySearch(request);
    }

    public List<Document> searchByDocType(String query, String docType) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .filterExpression("docType == '" + safeValue(docType) + "'")
                .build();

        return pgVectorVectorStore.similaritySearch(request);
    }

    public String formatKnowledgeContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "未检索到相关知识片段。";
        }

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            sb.append("【知识片段").append(i++).append("】\n");
            sb.append("docType=").append(metadata.getOrDefault("docType", "unknown")).append("\n");
            sb.append("topic=").append(metadata.getOrDefault("topic", "unknown")).append("\n");
            sb.append("filename=").append(metadata.getOrDefault("filename", "unknown")).append("\n");
            sb.append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    private String safeValue(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("'", "");
    }
}