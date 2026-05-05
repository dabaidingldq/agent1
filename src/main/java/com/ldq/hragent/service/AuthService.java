package com.ldq.hragent.service;

import com.ldq.hragent.model.auth.LoginRequest;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.auth.RegisterRequest;
import com.ldq.hragent.model.enums.ChatRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;

    public AuthService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoginUser login(LoginRequest request) {
        String username = safeTrim(request.getUsername());
        String password = safeTrim(request.getPassword());
        ChatRole role = request.getRole();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || role == null) {
            throw new IllegalArgumentException("账号、密码和身份不能为空");
        }

        List<LoginUser> users = jdbcTemplate.query("""
                SELECT id, username, display_name, role
                FROM sys_user
                WHERE username = ?
                  AND password_hash = ?
                  AND role = ?
                  AND status = 1
                LIMIT 1
                """,
                (rs, rowNum) -> new LoginUser(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        ChatRole.valueOf(rs.getString("role"))
                ),
                username, sha256(password), role.name()
        );

        if (users.isEmpty()) {
            throw new IllegalArgumentException("账号、密码或身份不正确");
        }

        return users.get(0);
    }

    public Long register(RegisterRequest request) {
        String username = safeTrim(request.getUsername());
        String password = safeTrim(request.getPassword());
        String displayName = safeTrim(request.getDisplayName());
        ChatRole role = request.getRole();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)
                || !StringUtils.hasText(displayName) || role == null) {
            throw new IllegalArgumentException("注册信息不完整");
        }

        Integer count = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM sys_user
                WHERE username = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, username);

        if (count != null && count > 0) {
            throw new IllegalArgumentException("账号已存在");
        }

        jdbcTemplate.update("""
                INSERT INTO sys_user (username, password_hash, display_name, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, NOW(), NOW())
                """,
                username, sha256(password), displayName, role.name()
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}