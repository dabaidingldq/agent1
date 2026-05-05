package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.exception.BizException;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.service.ApprovalManageService;
import com.ldq.hragent.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

/**
 * HR / 管理员审批管理接口
 *
 * 改造点：
 * 1. 不再信任前端 userId / role
 * 2. 当前登录身份从 Session 获取
 * 3. HR 和 ADMIN 可查询 / 审批
 * 4. reset 仅 ADMIN 可操作
 * 5. 所有查询 / 审批 / 重置行为写入 audit_log
 */
@RestController
@RequestMapping("/manage/approvals")
public class ApprovalManageController {

    private final ApprovalManageService approvalManageService;

    private final AuditLogService auditLogService;

    public ApprovalManageController(ApprovalManageService approvalManageService,
                                    AuditLogService auditLogService) {
        this.approvalManageService = approvalManageService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/pending")
    public BaseResponse<List<ApprovalManageService.ApprovalManageItem>> pending(
            @RequestParam(required = false) String businessType,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        String params = "businessType=" + safe(businessType);

        return withLoginUserContext(loginUser, "approval-pending",
                "查询待审批单",
                "approval",
                params,
                () -> {
                    List<ApprovalManageService.ApprovalManageItem> list =
                            approvalManageService.queryPendingApprovals(businessType);
                    return BaseResponse.success(list);
                });
    }

    @GetMapping("/all")
    public BaseResponse<List<ApprovalManageService.ApprovalManageItem>> all(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        String params = "businessType=" + safe(businessType)
                + ", status=" + safe(status);

        return withLoginUserContext(loginUser, "approval-all",
                "查询全部审批单",
                "approval",
                params,
                () -> {
                    List<ApprovalManageService.ApprovalManageItem> list =
                            approvalManageService.queryAllApprovals(businessType, status);
                    return BaseResponse.success(list);
                });
    }

    @PostMapping("/{approvalId}/approve")
    public BaseResponse<String> approve(
            @PathVariable Long approvalId,
            @RequestBody(required = false) ApprovalActionRequest request,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        String comment = request == null ? "" : safe(request.getComment());
        String params = "approvalId=" + approvalId + ", comment=" + comment;

        return withLoginUserContext(loginUser, "approval-approve",
                "审批通过",
                "approval",
                params,
                () -> BaseResponse.success(
                        approvalManageService.approveApproval(approvalId, comment)
                ));
    }

    @PostMapping("/{approvalId}/reject")
    public BaseResponse<String> reject(
            @PathVariable Long approvalId,
            @RequestBody(required = false) ApprovalActionRequest request,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        String comment = request == null ? "" : safe(request.getComment());
        String params = "approvalId=" + approvalId + ", comment=" + comment;

        return withLoginUserContext(loginUser, "approval-reject",
                "审批驳回",
                "approval",
                params,
                () -> BaseResponse.success(
                        approvalManageService.rejectApproval(approvalId, comment)
                ));
    }

    @GetMapping("/{approvalId}/summary")
    public BaseResponse<String> summary(
            @PathVariable Long approvalId,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        String params = "approvalId=" + approvalId;

        return withLoginUserContext(loginUser, "approval-summary",
                "查询审批摘要",
                "approval",
                params,
                () -> BaseResponse.success(
                        approvalManageService.getApprovalSummary(approvalId)
                ));
    }

    @PostMapping("/{approvalId}/reset")
    public BaseResponse<String> reset(
            @PathVariable Long approvalId,
            HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        if (loginUser.getRole() != ChatRole.ADMIN) {
            auditLogService.record(
                    loginUser,
                    "重置审批单",
                    "approval",
                    "approvalId=" + approvalId,
                    "无权限：只有管理员可以重置审批单状态",
                    false
            );
            throw new BizException("只有管理员可以重置审批单状态");
        }

        String params = "approvalId=" + approvalId;

        return withLoginUserContext(loginUser, "approval-reset",
                "重置审批单",
                "approval",
                params,
                () -> BaseResponse.success(
                        approvalManageService.resetApprovalToPending(approvalId)
                ));
    }

    private <T> BaseResponse<T> withLoginUserContext(LoginUser loginUser,
                                                     String chatId,
                                                     String operationName,
                                                     String operationModule,
                                                     String requestParams,
                                                     Supplier<BaseResponse<T>> action) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(loginUser.getUserId())
                    .tenantId("default")
                    .chatId(chatId)
                    .role(loginUser.getRole())
                    .build());

            BaseResponse<T> response = action.get();

            auditLogService.record(
                    loginUser,
                    operationName,
                    operationModule,
                    requestParams,
                    summarizeResponse(response),
                    true
            );

            return response;
        } catch (Exception e) {
            auditLogService.record(
                    loginUser,
                    operationName,
                    operationModule,
                    requestParams,
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
            throw new BizException("请先登录");
        }

        if (loginUser.getRole() != ChatRole.HR && loginUser.getRole() != ChatRole.ADMIN) {
            throw new BizException("当前身份无权访问审批管理接口");
        }

        return loginUser;
    }

    private String summarizeResponse(BaseResponse<?> response) {
        if (response == null) {
            return "";
        }

        Object data = response.getData();

        if (data instanceof List<?> list) {
            return "操作成功，共 " + list.size() + " 条记录";
        }

        return auditLogService.summarize(data);
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
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