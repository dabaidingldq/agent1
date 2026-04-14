package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.hr.ExpenseDraftResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ExpenseService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public ExpenseService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public ExpenseDraftResult createExpenseDraft(BigDecimal amount, String invoiceTitle, String expenseType, String remark) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ExpenseDraftResult(null, BigDecimal.ZERO, invoiceTitle, "REJECTED", "报销金额必须大于 0");
        }

        boolean titleValid = invoiceTitle != null && !invoiceTitle.isBlank() && !invoiceTitle.contains("个人");
        String status = titleValid ? "DRAFT" : "WARNING";

        String warningMessage = titleValid
                ? "报销草稿已创建。"
                : "报销草稿已创建，但当前发票抬头疑似不合规，请核对后再提交。";

        jdbcTemplate.update("""
                INSERT INTO expense_draft
                (user_id, amount, invoice_title, expense_type, remark, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                """,
                userId,
                amount,
                invoiceTitle,
                expenseType,
                remark,
                status
        );

        Long draftId = jdbcTemplate.query("""
                SELECT id
                FROM expense_draft
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        return new ExpenseDraftResult(
                draftId,
                amount,
                invoiceTitle,
                status,
                warningMessage
        );
    }
}