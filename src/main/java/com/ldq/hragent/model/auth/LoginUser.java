package com.ldq.hragent.model.auth;

import com.ldq.hragent.model.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户，存入 HttpSession
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long userId;

    private String username;

    private String displayName;

    private ChatRole role;
}