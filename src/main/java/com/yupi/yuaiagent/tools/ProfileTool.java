package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.service.PermissionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProfileTool {

    private static final Set<String> SAFE_FIELDS = Set.of(
            "手机号", "mobile",
            "紧急联系人", "emergencyContact",
            "家庭住址", "address",
            "学历信息", "education"
    );

    private final PermissionService permissionService;

    public ProfileTool(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Tool(description = "提交个人资料修改申请，例如手机号、紧急联系人、家庭住址等。敏感字段会转人工复核")
    public String updateProfile(String fieldName, String newValue) {
        permissionService.requireEmployeeOrAbove();

        if (fieldName == null || fieldName.isBlank()) {
            return "字段名不能为空。";
        }

        if ("idCardNo".equalsIgnoreCase(fieldName) || "身份证号".equals(fieldName)) {
            return "身份证号属于敏感字段，已转人工复核。";
        }

        if (!SAFE_FIELDS.contains(fieldName)) {
            return "该字段暂不支持自动修改，已建议转人工处理。";
        }

        return "资料修改申请已提交，字段：" + fieldName;
    }
}