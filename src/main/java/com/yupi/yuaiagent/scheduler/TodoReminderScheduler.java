package com.yupi.yuaiagent.scheduler;

import com.yupi.yuaiagent.service.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoReminderScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public TodoReminderScheduler(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /**
     * 每天 9:00 推送待办提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindDailyTodos() {
        List<Long> userIds = jdbcTemplate.query("""
                SELECT DISTINCT applicant_user_id
                FROM approval_instance
                WHERE status = 'PENDING'
                """, (rs, rowNum) -> rs.getLong(1));

        for (Long userId : userIds) {
            Integer count = jdbcTemplate.query("""
                    SELECT COUNT(1)
                    FROM approval_instance
                    WHERE applicant_user_id = ?
                      AND status = 'PENDING'
                    """, rs -> rs.next() ? rs.getInt(1) : 0, userId);

            if (count != null && count > 0) {
                notificationService.createNotification(
                        userId,
                        "待办任务提醒",
                        "您当前有 " + count + " 条待处理审批或事务，请及时查看。",
                        "TODO_REMINDER"
                );
            }
        }
    }
}