package com.yupi.yuaiagent.scheduler;

import com.yupi.yuaiagent.service.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProbationReminderScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public ProbationReminderScheduler(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /**
     * 每天 10:00 扫描未来 15 天到期的试用期
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void remindProbationDue() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT user_id, employee_name, probation_end_date
                FROM employee_profile
                WHERE probation_end_date IS NOT NULL
                  AND DATEDIFF(probation_end_date, CURDATE()) BETWEEN 0 AND 15
                """);

        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            String employeeName = String.valueOf(row.get("employee_name"));
            Object probationEndDate = row.get("probation_end_date");

            notificationService.createNotification(
                    userId,
                    "试用期转正提醒",
                    employeeName + " 的试用期将于 " + probationEndDate + " 到期，请提前准备转正评估。",
                    "PROBATION_REMINDER"
            );
        }
    }
}