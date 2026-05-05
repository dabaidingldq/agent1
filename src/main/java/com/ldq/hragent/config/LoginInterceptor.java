package com.ldq.hragent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldq.hragent.controller.AuthController;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.enums.ChatRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录与角色权限拦截器
 *
 * 核心原则：
 * 1. 页面和接口都必须登录后访问
 * 2. 员工只能访问 employee 端
 * 3. HR 只能访问 hr 端和审批管理接口
 * 4. 管理员可以访问 admin 端和 manage 管理接口
 * 5. /manage/approvals/** 特殊放行给 HR 和 ADMIN
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // 放行预检请求，避免 CORS / OPTIONS 被误拦截
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        LoginUser loginUser = (LoginUser) request.getSession()
                .getAttribute(AuthController.LOGIN_USER_SESSION_KEY);

        if (loginUser == null) {
            handleNoLogin(request, response);
            return false;
        }

        if (!hasRolePermission(uri, loginUser.getRole())) {
            writeJson(response, 403, "当前身份无权访问该资源");
            return false;
        }

        return true;
    }

    private boolean hasRolePermission(String uri, ChatRole role) {
        // 通用登录接口：只要已登录即可访问
        if (uri.startsWith("/auth/")) {
            return true;
        }

        // 聊天历史接口：根据 Session 在 Controller / Service 中继续按 userId + role 过滤
        if (uri.startsWith("/chat/")) {
            return true;
        }

        // 员工端页面和员工 AI 接口
        if (uri.startsWith("/employee/") || uri.startsWith("/ai/employee/")) {
            return role == ChatRole.EMPLOYEE;
        }

        // HR 端页面和 HR AI 接口
        if (uri.startsWith("/hr/") || uri.startsWith("/ai/hr/")) {
            return role == ChatRole.HR;
        }

        // 审批管理接口：HR 和管理员都可以访问
        if (uri.startsWith("/manage/approvals/")) {
            return role == ChatRole.HR || role == ChatRole.ADMIN;
        }

        // 管理员端页面、管理员 AI 接口、其他管理接口
        if (uri.startsWith("/admin/")
                || uri.startsWith("/ai/admin/")
                || uri.startsWith("/manage/")) {
            return role == ChatRole.ADMIN;
        }

        return true;
    }

    private void handleNoLogin(HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        boolean isPageRequest = uri.endsWith(".html")
                || accept != null && accept.contains("text/html");

        if (isPageRequest) {
            response.sendRedirect("/login.html");
            return;
        }

        writeJson(response, 401, "请先登录");
    }

    private void writeJson(HttpServletResponse response,
                           int code,
                           String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}