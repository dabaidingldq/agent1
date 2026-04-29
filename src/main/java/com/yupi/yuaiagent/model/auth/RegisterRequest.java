package com.yupi.yuaiagent.model.auth;

import com.yupi.yuaiagent.model.enums.ChatRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    @NotNull(message = "身份不能为空")
    private ChatRole role;
}