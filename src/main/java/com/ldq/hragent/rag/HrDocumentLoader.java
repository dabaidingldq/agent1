package com.ldq.hragent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HrDocumentLoader {

    private static final String[] DOCUMENT_PATTERNS = {
            "classpath:document/policy/**/*.md",
            "classpath:document/process/**/*.md",
            "classpath:document/compliance/**/*.md",
            "classpath:document/holiday/**/*.md",
            "classpath:document/shuttle/**/*.md"
    };

    private final ResourcePatternResolver resourcePatternResolver;

    public HrDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        for (String pattern : DOCUMENT_PATTERNS) {
            allDocuments.addAll(loadByPattern(pattern));
        }
        log.info("HR knowledge documents loaded, size={}", allDocuments.size());
        return allDocuments;
    }

    private List<Document> loadByPattern(String pattern) {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String resourcePath = resource.getURI().toString();

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("resourcePath", resourcePath)
                        .withAdditionalMetadata("docType", parseDocType(resourcePath))
                        .withAdditionalMetadata("topic", parseTopic(resourcePath))
                        .withAdditionalMetadata("audience", parseAudience(resourcePath))
                        .withAdditionalMetadata("effectiveDate", "2026-01-01")
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                documents.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败, pattern={}", pattern, e);
        }
        return documents;
    }

    private String parseDocType(String path) {
        if (path.contains("/policy/")) {
            return "policy";
        }
        if (path.contains("/process/")) {
            return "process";
        }
        if (path.contains("/compliance/")) {
            return "compliance";
        }
        if (path.contains("/holiday/")) {
            return "holiday";
        }
        if (path.contains("/shuttle/")) {
            return "shuttle";
        }
        return "general";
    }

    private String parseTopic(String path) {
        if (path.contains("/leave/")) {
            return "leave";
        }
        if (path.contains("/payroll/")) {
            return "payroll";
        }
        if (path.contains("/benefits/")) {
            return "benefits";
        }
        if (path.contains("/travel/")) {
            return "travel";
        }
        if (path.contains("/onboarding/")) {
            return "onboarding";
        }
        if (path.contains("/offboarding/")) {
            return "offboarding";
        }
        if (path.contains("/transfer/")) {
            return "transfer";
        }
        return "general";
    }
    private String parseAudience(String path) {
        if (path.contains("/admin/")) {
            return "admin";
        }
        if (path.contains("/hr/")) {
            return "hr";
        }
        return "all";
    }
}