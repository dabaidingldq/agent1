package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.CertificateApplyResult;
import com.ldq.hragent.service.CertificateService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CertificateTool {

    private final CertificateService certificateService;

    public CertificateTool(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Tool(description = """
            为当前登录用户创建证明开具申请。
            适用场景：
            - 用户说“我要开在职证明”
            - 用户说“帮我申请收入证明 / 实习证明 / 工作证明”
            - 用户通过快捷入口提交证明开具申请
            
            参数规则：
            - certificateType：证明类型，可选：在职证明、收入证明、实习证明
            - purpose：用途，例如签证、租房、贷款、入学、落户等
            - 如果用户没有说明证明类型，应先追问
            - 如果用户没有说明用途，可以继续创建，但建议提示后续可能需要 HR 补充确认
            - 不要编造证明内容，只能提交申请或说明流程
            """)
    public CertificateApplyResult applyCertificate(String certificateType, String purpose) {
        return certificateService.applyCertificate(certificateType, purpose);
    }
}