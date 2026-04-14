package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.CertificateApplyResult;
import com.yupi.yuaiagent.service.CertificateService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CertificateTool {

    private final CertificateService certificateService;

    public CertificateTool(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Tool(description = "为当前登录用户创建证明开具申请。certificateType 可选：在职证明、收入证明、实习证明")
    public CertificateApplyResult applyCertificate(String certificateType, String purpose) {
        return certificateService.applyCertificate(certificateType, purpose);
    }
}