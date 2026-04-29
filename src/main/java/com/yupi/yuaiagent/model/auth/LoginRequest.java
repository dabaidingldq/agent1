package com.yupi.yuaiagent.model.auth;

import com.yupi.yuaiagent.model.enums.ChatRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotNull(message = "身份不能为空")
    private ChatRole role;
}