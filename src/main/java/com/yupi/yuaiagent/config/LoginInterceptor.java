package com.yupi.yuaiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.controller.AuthController;
import com.yupi.yuaiagent.model.auth.LoginUser;
import com.yupi.yuaiagent.model.enums.ChatRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

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
        if (uri.startsWith("/employee/") || uri.startsWith("/ai/employee/")) {
            return role == ChatRole.EMPLOYEE;
        }
        if (uri.startsWith("/hr/") || uri.startsWith("/ai/hr/")) {
            return role == ChatRole.HR;
        }
        if (uri.startsWith("/admin/") || uri.startsWith("/ai/admin/")
                || uri.startsWith("/admin/")
                || uri.startsWith("/manage/")) {
            return role == ChatRole.ADMIN;
        }
        return true;
    }

    private void handleNoLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}