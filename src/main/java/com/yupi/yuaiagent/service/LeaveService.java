package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.exception.BizException;
import com.yupi.yuaiagent.model.hr.LeaveBalanceResult;
import com.yupi.yuaiagent.model.hr.LeaveRequestResult;
import com.yupi.yuaiagent.model.hr.LeaveValidationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LeaveService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public LeaveService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public LeaveBalanceResult queryMyLeaveBalance() {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        String sql = """
                SELECT annual_leave_remaining,
                       personal_leave_used,
                       sick_leave_used,
                       comp_off_remaining
                FROM employee_leave_balance
                WHERE user_id = ?
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return new LeaveBalanceResult(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "暂未查询到您的休假额度，请联系 HR 核实初始化数据。"
                );
            }
            return new LeaveBalanceResult(
                    defaultDecimal(rs.getBigDecimal("annual_leave_remaining")),
                    defaultDecimal(rs.getBigDecimal("personal_leave_used")),
                    defaultDecimal(rs.getBigDecimal("sick_leave_used")),
                    defaultDecimal(rs.getBigDecimal("comp_off_remaining")),
                    "年假、事假、病假、调休的具体适用规则请以公司制度和当地法规为准。"
            );
        }, userId);
    }

    public LeaveValidationResult validateLeaveRequest(String leaveType, String startTime, String endTime) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        if (leaveType == null || leaveType.isBlank()) {
            return new LeaveValidationResult(false, "请假类型不能为空", false, false);
        }
        if (startTime == null || endTime == null) {
            return new LeaveValidationResult(false, "请假开始和结束时间不能为空", false, false);
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startTime);
            end = LocalDateTime.parse(endTime);
        } catch (Exception e) {
            return new LeaveValidationResult(false, "时间格式不正确，请使用 ISO-8601 格式，例如 2026-04-18T13:00:00", false, false);
        }

        if (!end.isAfter(start)) {
            return new LeaveValidationResult(false, "结束时间必须晚于开始时间", false, false);
        }

        boolean conflict = hasTimeConflict(userId, start, end);
        if (conflict) {
            return new LeaveValidationResult(false, "该时间段已存在请假记录，存在时间冲突", true, false);
        }

        boolean balanceEnough = checkBalanceEnough(userId, leaveType, start, end);
        if (!balanceEnough) {
            return new LeaveValidationResult(false, "当前假期额度不足，无法创建申请", false, false);
        }

        return new LeaveValidationResult(true, "校验通过，可以创建请假申请", false, true);
    }

    public LeaveRequestResult createLeaveRequest(String leaveType, String startTime, String endTime, String reason) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        LeaveValidationResult validationResult = validateLeaveRequest(leaveType, startTime, endTime);
        if (!validationResult.valid()) {
            throw new BizException(validationResult.message());
        }

        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);

        String insertSql = """
                INSERT INTO leave_request
                (user_id, leave_type, start_time, end_time, reason, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;

        jdbcTemplate.update(insertSql,
                userId,
                leaveType,
                start,
                end,
                reason == null ? "" : reason,
                "PENDING");

        Long requestId = jdbcTemplate.query("""
                SELECT id
                FROM leave_request
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        jdbcTemplate.update("""
                INSERT INTO approval_instance
                (applicant_user_id, business_type, status, current_node, stay_days, estimated_remaining_time, can_remind, created_at, updated_at)
                VALUES (?, 'LEAVE', 'PENDING', '直属经理审批', 0, '1个工作日', 1, NOW(), NOW())
                """, userId);

        return new LeaveRequestResult(
                requestId,
                leaveType,
                start.toString(),
                end.toString(),
                "PENDING",
                "请假申请已创建，等待审批。"
        );
    }

    private boolean hasTimeConflict(Long userId, LocalDateTime start, LocalDateTime end) {
        Integer count = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM leave_request
                WHERE user_id = ?
                  AND status IN ('PENDING', 'APPROVED')
                  AND start_time < ?
                  AND end_time > ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, userId, end, start);
        return count != null && count > 0;
    }

    private boolean checkBalanceEnough(Long userId, String leaveType, LocalDateTime start, LocalDateTime end) {
        double leaveDays = Math.max(0.5, Duration.between(start, end).toHours() / 8.0);

        if (leaveType.contains("年假")) {
            BigDecimal balance = jdbcTemplate.query("""
                    SELECT annual_leave_remaining
                    FROM employee_leave_balance
                    WHERE user_id = ?
                    LIMIT 1
                    """, rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);
            return balance != null && balance.doubleValue() >= leaveDays;
        }

        if (leaveType.contains("调休")) {
            BigDecimal balance = jdbcTemplate.query("""
                    SELECT comp_off_remaining
                    FROM employee_leave_balance
                    WHERE user_id = ?
                    LIMIT 1
                    """, rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);
            return balance != null && balance.doubleValue() >= leaveDays;
        }

        // 事假、病假先默认允许，后续可继续细化
        return true;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}