package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.model.hr.ApprovalHistoryResult;
import com.ldq.hragent.model.hr.ApprovalProgressResult;
import com.ldq.hragent.service.ApprovalHistoryService;
import com.ldq.hragent.service.ApprovalService;
import com.ldq.hragent.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工端审批进度接口
 *
 * 改造点：
 * 1. 不再信任前端 userId
 * 2. 当前员工身份从 Session 获取
 * 3. 所有查询 / 催办行为写入 audit_log
 */
@RestController
@RequestMapping("/employee/approvals")
public class EmployeeApprovalController {

    private final ApprovalHistoryService approvalHistoryService;

    private final ApprovalService approvalService;

    private final AuditLogService auditLogService;

    public EmployeeApprovalController(ApprovalHistoryService approvalHistoryService,
                                      ApprovalService approvalService,
                                      AuditLogService auditLogService) {
        this.approvalHistoryService = approvalHistoryService;
        this.approvalService = approvalService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/latest")
    public BaseResponse<ApprovalProgressResult> latest(@RequestParam(required = false) String businessType,
                                                       HttpSession session) {
        LoginUser loginUser = getLoginUser(session);
        String params = "businessType=" + safe(businessType);

        try {
            setEmployeeContext(loginUser, "employee-approval-latest");

            ApprovalProgressResult result = approvalService.queryLatestMyApproval(businessType);

            auditLogService.record(
                    loginUser,
                    "查询最近审批进度",
                    "approval",
                    params,
                    auditLogService.summarize(result),
                    true
            );

            return BaseResponse.success(result);
        } catch (Exception e) {
            auditLogService.record(
                    loginUser,
                    "查询最近审批进度",
                    "approval",
                    params,
                    e.getMessage(),
                    false
            );
            throw e;
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @GetMapping("/history")
    public BaseResponse<List<ApprovalHistoryResult>> history(@RequestParam(required = false) String businessType,
                                                             @RequestParam(required = false) String month,
                                                             @RequestParam(required = false) String status,
                                                             HttpSession session) {
        LoginUser loginUser = getLoginUser(session);
        String params = "businessType=" + safe(businessType)
                + ", month=" + safe(month)
                + ", status=" + safe(status);

        try {
            setEmployeeContext(loginUser, "employee-approval-history");

            List<ApprovalHistoryResult> list = approvalHistoryService.queryMyApprovalHistory(businessType, month);

            if (status != null && !status.isBlank()) {
                String expectedStatus = status.trim();
                list = list.stream()
                        .filter(item -> item.status() != null
                                && expectedStatus.equalsIgnoreCase(item.status()))
                        .toList();
            }

            auditLogService.record(
                    loginUser,
                    "查询本人审批历史",
                    "approval",
                    params,
                    "查询成功，共 " + list.size() + " 条记录",
                    true
            );

            return BaseResponse.success(list);
        } catch (Exception e) {
            auditLogService.record(
                    loginUser,
                    "查询本人审批历史",
                    "approval",
                    params,
                    e.getMessage(),
                    false
            );
            throw e;
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @PostMapping("/{approvalId}/remind")
    public BaseResponse<String> remind(@PathVariable Long approvalId,
                                       HttpSession session) {
        LoginUser loginUser = getLoginUser(session);
        String params = "approvalId=" + approvalId;

        try {
            setEmployeeContext(loginUser, "employee-approval-remind");

            String result = approvalService.remindCurrentApprover(approvalId);

            auditLogService.record(
                    loginUser,
                    "催办审批单",
                    "approval",
                    params,
                    result,
                    true
            );

            return BaseResponse.success(result);
        } catch (Exception e) {
            auditLogService.record(
                    loginUser,
                    "催办审批单",
                    "approval",
                    params,
                    e.getMessage(),
                    false
            );
            throw e;
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    private LoginUser getLoginUser(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.LOGIN_USER_SESSION_KEY);

        if (loginUser == null) {
            throw new IllegalStateException("请先登录");
        }

        if (loginUser.getRole() != ChatRole.EMPLOYEE) {
            throw new IllegalStateException("当前身份无权访问员工审批接口");
        }

        return loginUser;
    }

    private void setEmployeeContext(LoginUser loginUser, String chatId) {
        HrRequestContextHolder.setContext(HrRequestContext.builder()
                .userId(loginUser.getUserId())
                .tenantId("default")
                .chatId(chatId)
                .role(ChatRole.EMPLOYEE)
                .build());
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }
}