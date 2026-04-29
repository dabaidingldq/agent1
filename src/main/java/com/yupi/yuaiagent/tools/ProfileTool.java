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

    @Tool(description = """
            提交当前登录用户的个人资料修改申请。
            适用场景：
            - 用户说“修改我的手机号”
            - 用户说“更新紧急联系人”
            - 用户说“修改家庭住址”
            - 用户说“更新学历信息”
            
            参数规则：
            - fieldName：要修改的字段名，例如 手机号、紧急联系人、家庭住址、学历信息
            - newValue：新值
            - 如果缺少字段名或新值，应先追问
            
            安全规则：
            - 身份证号、银行卡号、工资卡、敏感身份信息不能自动修改
            - 敏感字段应转人工复核
            - 只能修改当前登录用户本人资料，不能修改他人资料
            """)
    public String updateProfile(String fieldName, String newValue) {
        permissionService.requireEmployeeOrAbove();

        if (fieldName == null || fieldName.isBlank()) {
            return "字段名不能为空。";
        }

        if (newValue == null || newValue.isBlank()) {
            return "新值不能为空。";
        }

        String normalizedField = fieldName.trim();

        if ("idCardNo".equalsIgnoreCase(normalizedField)
                || "身份证号".equals(normalizedField)
                || "银行卡号".equals(normalizedField)
                || "工资卡".equals(normalizedField)) {
            return "该字段属于敏感字段，不能自动修改，已建议转人工复核。";
        }

        if (!SAFE_FIELDS.contains(normalizedField)) {
            return "该字段暂不支持自动修改，已建议转人工处理。字段：" + normalizedField;
        }

        return "资料修改申请已提交，字段：" + normalizedField + "。";
    }
}