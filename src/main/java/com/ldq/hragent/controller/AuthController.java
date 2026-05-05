package com.ldq.hragent.controller;

import com.ldq.hragent.model.auth.LoginRequest;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.auth.RegisterRequest;
import com.ldq.hragent.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public static final String LOGIN_USER_SESSION_KEY = "LOGIN_USER";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginRequest request, HttpSession session) {
        LoginUser loginUser = authService.login(request);
        session.setAttribute(LOGIN_USER_SESSION_KEY, loginUser);
        session.setMaxInactiveInterval(60 * 60 * 2);

        Map<String, Object> data = new HashMap<>();
        data.put("user", loginUser);
        data.put("redirectUrl", getRedirectUrl(loginUser));

        return success("登录成功", data);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterRequest request) {
        Long userId = authService.register(request);
        return success("注册成功", Map.of("userId", userId));
    }

    @GetMapping("/current")
    public Map<String, Object> current(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            return fail(401, "未登录");
        }
        return success("已登录", Map.of(
                "user", loginUser,
                "redirectUrl", getRedirectUrl(loginUser)
        ));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return success("退出成功", null);
    }

    private String getRedirectUrl(LoginUser user) {
        return switch (user.getRole()) {
            case EMPLOYEE -> "/employee/chat.html";
            case HR -> "/hr/chat.html";
            case ADMIN -> "/admin/chat.html";
        };
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> fail(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        return result;
    }
}