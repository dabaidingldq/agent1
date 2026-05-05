package com.ldq.hragent.controller;

import com.ldq.hragent.service.HrKnowledgeService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/debug/knowledge")
public class KnowledgeDebugController {

    private final HrKnowledgeService hrKnowledgeService;

    public KnowledgeDebugController(HrKnowledgeService hrKnowledgeService) {
        this.hrKnowledgeService = hrKnowledgeService;
    }

    @GetMapping("/search")
    public List<Document> search(@RequestParam String query,
                                 @RequestParam(required = false) String topic,
                                 @RequestParam(required = false) String docType) {
        if (topic != null && !topic.isBlank()) {
            return hrKnowledgeService.searchByTopic(query, topic);
        }
        if (docType != null && !docType.isBlank()) {
            return hrKnowledgeService.searchByDocType(query, docType);
        }
        return hrKnowledgeService.search(query);
    }
}