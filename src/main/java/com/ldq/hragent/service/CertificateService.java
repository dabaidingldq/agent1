package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.CertificateApplyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public CertificateService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }
    @com.ldq.hragent.aop.AuditLog(operationName = "提交证明申请", operationModule = "certificate")

    public CertificateApplyResult applyCertificate(String certificateType, String purpose) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        if (certificateType == null || certificateType.isBlank()) {
            return new CertificateApplyResult(null, null, "REJECTED", "证明类型不能为空");
        }

        String normalizedType = normalizeCertificateType(certificateType);
        boolean needManualReview = "收入证明".equals(normalizedType);

        String status = needManualReview ? "PENDING_REVIEW" : "PENDING";

        jdbcTemplate.update("""
                INSERT INTO certificate_request
                (user_id, certificate_type, purpose, status, need_manual_review, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                """,
                userId,
                normalizedType,
                purpose == null ? "" : purpose,
                status,
                needManualReview ? 1 : 0
        );

        Long requestId = jdbcTemplate.query("""
                SELECT id
                FROM certificate_request
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        jdbcTemplate.update("""
                INSERT INTO approval_instance
                (applicant_user_id, business_type, status, current_node, stay_days, estimated_remaining_time, can_remind, created_at, updated_at)
                VALUES (?, 'CERTIFICATE', ?, ?, 0, '1个工作日', 1, NOW(), NOW())
                """,
                userId,
                status,
                needManualReview ? "HR复核" : "系统处理中"
        );

        String msg = needManualReview
                ? "证明申请已提交。由于收入证明涉及敏感信息，已进入 HR 人工复核流程。"
                : "证明申请已提交，系统将继续处理。后续可接入 PDF 生成与电子章。";

        return new CertificateApplyResult(
                requestId,
                normalizedType,
                status,
                msg
        );
    }

    private String normalizeCertificateType(String certificateType) {
        String value = certificateType.trim();
        if (value.contains("在职")) {
            return "在职证明";
        }
        if (value.contains("收入")) {
            return "收入证明";
        }
        if (value.contains("实习")) {
            return "实习证明";
        }
        return value;
    }
}