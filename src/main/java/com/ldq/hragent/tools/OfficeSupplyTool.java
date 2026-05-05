package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.OfficeSupplyApplyResult;
import com.ldq.hragent.service.OfficeSupplyService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OfficeSupplyTool {

    private final OfficeSupplyService officeSupplyService;

    public OfficeSupplyTool(OfficeSupplyService officeSupplyService) {
        this.officeSupplyService = officeSupplyService;
    }

    @Tool(description = """
            为当前登录员工创建办公用品申请。
            适用场景：
            - 用户说“我要申请办公用品”
            - 用户说“帮我申请键盘、鼠标、打印纸、笔记本、签字笔”等
            - 用户通过快捷入口提交办公用品申请
            
            参数规则：
            - items：办公用品清单描述，例如“两支黑色签字笔和一个笔记本”
            - 如果用户没有说明具体物品或数量，应先追问
            - 不需要用户提供 userId，系统会使用当前登录用户身份
            - 不允许替其他员工提交申请，除非后端服务明确支持
            - 工具返回后，应告诉用户申请是否已创建、审批状态和后续步骤
            """)
    public OfficeSupplyApplyResult applyOfficeSupplies(String items) {
        return officeSupplyService.applyOfficeSupplies(items);
    }
}