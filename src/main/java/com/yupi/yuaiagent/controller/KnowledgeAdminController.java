package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.service.HrKnowledgeAdminService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/knowledge")
public class KnowledgeAdminController {

    private final HrKnowledgeAdminService hrKnowledgeAdminService;

    public KnowledgeAdminController(HrKnowledgeAdminService hrKnowledgeAdminService) {
        this.hrKnowledgeAdminService = hrKnowledgeAdminService;
    }

    @PostMapping("/rebuild")
    public BaseResponse<String> rebuild(@RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("knowledge-admin")
                    .role(ChatRole.ADMIN)
                    .build());
            return BaseResponse.success(hrKnowledgeAdminService.rebuildKnowledgeBase());
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @GetMapping("/count")
    public BaseResponse<Integer> count(@RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("knowledge-admin")
                    .role(ChatRole.ADMIN)
                    .build());
            return BaseResponse.success(hrKnowledgeAdminService.countKnowledgeRows());
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam Long userId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String topic,
                                       @RequestParam(required = false) String docType) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("knowledge-upload")
                    .role(ChatRole.ADMIN)
                    .build());
            return BaseResponse.success(
                    hrKnowledgeAdminService.uploadKnowledgeFile(file, topic, docType)
            );
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}