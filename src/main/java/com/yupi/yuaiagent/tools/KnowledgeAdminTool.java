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

    @Tool(description = """
            触发 HR 知识库重建，仅管理员可用。
            适用场景：
            - 管理员说“重建知识库”
            - 管理员说“刷新 RAG 向量库”
            - 管理员说“上传知识后重新索引”
            
            调用规则：
            - 这是管理员写操作
            - 用户只是询问“知识库是什么”时，不应调用本工具
            - 调用后应说明重建任务是否已触发，以及可能需要等待
            """)
    public String rebuildKnowledgeIndex() {
        return hrKnowledgeAdminService.rebuildKnowledgeBase();
    }

    @Tool(description = """
            查询当前 HR 知识库中的向量条目数量，仅管理员可用。
            适用场景：
            - 管理员说“知识库有多少条”
            - 管理员说“向量库条目数”
            - 管理员想确认知识库是否已写入数据
            
            调用规则：
            - 只查询数量
            - 不返回具体知识内容
            """)
    public Integer countKnowledgeRows() {
        return hrKnowledgeAdminService.countKnowledgeRows();
    }
}