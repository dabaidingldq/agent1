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

    @Tool(description = "发布政策变更公告，仅 HR 或管理员可用。targetScope 可选 ALL、HR、ADMIN")
    public PolicyAnnouncementResult publishAnnouncement(String title, String summary, String targetScope) {
        return announcementService.publishAnnouncement(title, summary, targetScope);
    }
}