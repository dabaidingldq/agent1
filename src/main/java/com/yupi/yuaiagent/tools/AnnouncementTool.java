package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.PolicyAnnouncementResult;
import com.yupi.yuaiagent.service.AnnouncementService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementTool {

    private final AnnouncementService announcementService;

    public AnnouncementTool(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Tool(description = """
            发布政策变更公告，仅 HR 或管理员可用。
            适用场景：
            - HR 或管理员说“发布公告”
            - HR 或管理员说“通知全员某个政策变化”
            - HR 或管理员说“发布 HR 政策变更通知”
            
            参数规则：
            - title：公告标题
            - summary：公告摘要或正文简要内容
            - targetScope：公告范围，可选 ALL、HR、ADMIN
            - 面向全员时 targetScope=ALL
            - 面向 HR 时 targetScope=HR
            - 面向管理员时 targetScope=ADMIN
            
            安全规则：
            - 普通员工不能发布公告
            - 如果缺少标题或内容，应先追问
            - 发布后应说明公告标题、范围和发布结果
            """)
    public PolicyAnnouncementResult publishAnnouncement(String title, String summary, String targetScope) {
        return announcementService.publishAnnouncement(title, summary, normalizeScope(targetScope));
    }

    private String normalizeScope(String targetScope) {
        if (targetScope == null || targetScope.isBlank()) {
            return "ALL";
        }

        String text = targetScope.trim().toUpperCase();

        if (text.contains("全员") || text.contains("所有") || text.contains("ALL")) {
            return "ALL";
        }
        if (text.contains("HR")) {
            return "HR";
        }
        if (text.contains("管理员") || text.contains("ADMIN")) {
            return "ADMIN";
        }

        return text;
    }
}