package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.service.HrKnowledgeAdminService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeAdminTool {

    private final HrKnowledgeAdminService hrKnowledgeAdminService;

    public KnowledgeAdminTool(HrKnowledgeAdminService hrKnowledgeAdminService) {
        this.hrKnowledgeAdminService = hrKnowledgeAdminService;
    }

    @Tool(description = "触发 HR 知识库重建，仅管理员可用")
    public String rebuildKnowledgeIndex() {
        return hrKnowledgeAdminService.rebuildKnowledgeBase();
    }

    @Tool(description = "查询当前知识库中的向量条目数量，仅管理员可用")
    public Integer countKnowledgeRows() {
        return hrKnowledgeAdminService.countKnowledgeRows();
    }
}