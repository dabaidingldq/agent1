package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.exception.BizException;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.service.ApprovalManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/manage/approvals")
public class ApprovalManageController {

    private final ApprovalManageService approvalManageService;

    public ApprovalManageController(ApprovalManageService approvalManageService) {
        this.approvalManageService = approvalManageService;
    }

    @GetMapping("/pending")
    public BaseResponse<List<ApprovalManageService.ApprovalManageItem>> pending(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam(required = false) String businessType) {
        return withContext(userId, role, "approval-pending",
                () -> BaseResponse.success(approvalManageService.queryPendingApprovals(businessType)));
    }

    @GetMapping("/all")
    public BaseResponse<List<ApprovalManageService.ApprovalManageItem>> all(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status) {
        return withContext(userId, role, "approval-all",
                () -> BaseResponse.success(approvalManageService.queryAllApprovals(businessType, status)));
    }

    @PostMapping("/{approvalId}/approve")
    public BaseResponse<String> approve(
            @PathVariable Long approvalId,
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestBody(required = false) ApprovalActionRequest request) {
        return withContext(userId, role, "approval-approve",
                () -> BaseResponse.success(
                        approvalManageService.approveApproval(
                                approvalId,
                                request == null ? "" : request.getComment()
                        )));
    }

    @PostMapping("/{approvalId}/reject")
    public BaseResponse<String> reject(
            @PathVariable Long approvalId,
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestBody(required = false) ApprovalActionRequest request) {
        return withContext(userId, role, "approval-reject",
                () -> BaseResponse.success(
                        approvalManageService.rejectApproval(
                                approvalId,
                                request == null ? "" : request.getComment()
                        )));
    }

    private <T> BaseResponse<T> withContext(Long userId, String role, String chatId, Supplier<BaseResponse<T>> action) {
        try {
            ChatRole chatRole = parseRole(role);
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId(chatId)
                    .role(chatRole)
                    .build());
            return action.get();
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    private ChatRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BizException("role 不能为空");
        }
        try {
            return ChatRole.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new BizException("role 仅支持 EMPLOYEE / HR / ADMIN");
        }
    }

    public static class ApprovalActionRequest {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}