package com.ldq.hragent.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    private final JdbcTemplate jdbcTemplate;

    public OrganizationService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isTeamLead(Long userId) {
        Boolean result = jdbcTemplate.query("""
                SELECT is_team_lead
                FROM employee_profile
                WHERE user_id = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getBoolean(1) : false, userId);
        return result != null && result;
    }
}