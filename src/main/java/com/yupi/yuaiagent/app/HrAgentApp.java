package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.service.ChatSessionService;
import com.yupi.yuaiagent.utils.DateContextUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class HrAgentApp {

    private static final String BASE_SYSTEM_PROMPT = """
你是企业内部智能行政 / HR 助手。
你的目标是围绕“知识问答 + 业务执行 + 审批追踪 + 主动提醒”四类任务，为用户提供准确、克制、可审计的服务。

【当前时间规则】
当前日期、当前月份以每次用户请求中的“运行上下文”为准。
当用户提到“今年、本月、这个月、最近、当前、现在”等时间表达时，必须根据运行上下文中的当前日期进行理解。
不得凭模型自身知识判断当前年份或月份。

【工具调用总规则】
1. 涉及请假、审批、办公用品、证明开具、报销、会议室、访客、消息通知、数据看板、知识库管理等业务操作时，必须优先调用工具，不允许仅凭常识回答。
2. 用户已经给出必要条件时，必须立即调用工具，不要继续追问。
3. 用户缺少必要条件时，只追问缺失字段，不要泛泛解释。
4. 工具返回空结果时，只能说明“系统未查询到记录”，不能自行推测数据库没有数据的原因。
5. 工具返回无权限、需要人工复核、已处理、未找到等结果时，必须如实告知，不得绕过。
6. 工具返回具体业务数据时，要整理成人能看懂的列表，不要直接输出数据库字段名或原始 JSON。
7. 不得编造不存在的审批单、政策、员工信息、金额、日期、状态。

【审批类工具调用规则】
1. 用户说“历史审批记录、审批历史、最近的历史审批、请假的历史审批、办公用品历史审批、证明历史审批”等，必须调用审批历史工具。
2. HR 或管理员查询审批历史时，优先调用 queryTeamApprovalHistory。
3. 普通员工查询自己的审批历史时，调用 queryMyApprovalHistory。
4. 用户说“请假”时，businessType = LEAVE。
5. 用户说“办公用品”时，businessType = OFFICE_SUPPLY。
6. 用户说“在职证明、证明开具、证明”时，businessType = CERTIFICATE。
7. 用户说“报销”时，businessType = EXPENSE。
8. 用户给出“某年某月、yyyy-MM、今年某月、本月”时，month 必须转换为 yyyy-MM 格式。
9. 如果用户只说“最近的历史审批记录”，没有指定月份，则默认查询运行上下文中的当前月份。
10. 如果用户已经说了业务类型和月份，不要再要求用户补充月份，直接调用工具。

【知识库规则】
涉及公司政策、员工手册、流程规范时，优先基于知识库内容回答，不允许凭空编造。
如果知识库没有命中，应说明“知识库未检索到明确依据”，不要自造制度。

【安全规则】
对于身份证号、工资明细、住址、联系方式、他人敏感数据、敏感审批信息等内容，不得自行猜测，不得主动扩展。
不同角色只能访问自己权限范围内的数据。

回答风格要专业、简洁、面向企业场景。
""";

    private final ChatClient chatClient;

    @Resource
    private com.yupi.yuaiagent.rag.HrRagAdvisorFactory hrRagAdvisorFactory;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    @Qualifier("pgVectorVectorStore")
    private VectorStore hrVectorStore;

    @Resource
    @Qualifier("employeeTools")
    private ToolCallback[] employeeTools;

    @Resource
    @Qualifier("hrTools")
    private ToolCallback[] hrTools;

    @Resource
    @Qualifier("adminTools")
    private ToolCallback[] adminTools;

    public HrAgentApp(ChatModel dashscopeChatModel,
                      JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(BASE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(ChatRequest request) {
        validateRequest(request);

        HrRequestContext context = HrRequestContext.builder()
                .chatId(request.getChatId())
                .userId(request.getUserId())
                .tenantId(request.getTenantId() == null ? "default" : request.getTenantId())
                .role(request.getRole())
                .build();

        try {
            HrRequestContextHolder.setContext(context);

            chatSessionService.createSessionIfAbsent(
                    request.getChatId(),
                    request.getUserId(),
                    request.getRole().name(),
                    request.getMessage()
            );
            chatSessionService.saveUserMessage(
                    request.getChatId(),
                    request.getUserId(),
                    request.getMessage()
            );

            ChatResponse response = chatClient
                    .prompt()
                    .system(buildRoleSystemPrompt(request))
                    .user(buildSafeUserPrompt(request))
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, request.getChatId()))
                    .advisors(resolveAdvisorByMessage(request.getMessage()))
                    .toolCallbacks(getToolsByRole(request.getRole()))
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();

            chatSessionService.saveAssistantMessage(
                    request.getChatId(),
                    request.getUserId(),
                    content
            );

            log.info("hr agent sync response, userId={}, role={}, content={}",
                    request.getUserId(), request.getRole(), content);
            return content;
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    public Flux<String> doChatByStream(ChatRequest request) {
        validateRequest(request);

        HrRequestContext context = HrRequestContext.builder()
                .chatId(request.getChatId())
                .userId(request.getUserId())
                .tenantId(request.getTenantId() == null ? "default" : request.getTenantId())
                .role(request.getRole())
                .build();

        HrRequestContextHolder.setContext(context);

        chatSessionService.createSessionIfAbsent(
                request.getChatId(),
                request.getUserId(),
                request.getRole().name(),
                request.getMessage()
        );
        chatSessionService.saveUserMessage(
                request.getChatId(),
                request.getUserId(),
                request.getMessage()
        );

        StringBuilder assistantBuffer = new StringBuilder();

        return chatClient
                .prompt()
                .system(buildRoleSystemPrompt(request))
                .user(buildSafeUserPrompt(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, request.getChatId()))
                .advisors(resolveAdvisorByMessage(request.getMessage()))
                .toolCallbacks(getToolsByRole(request.getRole()))
                .stream()
                .content()
                .doOnNext(assistantBuffer::append)
                .doFinally(signalType -> {
                    try {
                        String finalContent = assistantBuffer.toString();
                        if (!finalContent.isBlank()) {
                            chatSessionService.saveAssistantMessage(
                                    request.getChatId(),
                                    request.getUserId(),
                                    finalContent
                            );
                        }
                    } finally {
                        HrRequestContextHolder.clear();
                    }
                });
    }

    public String doChat(String message, String chatId) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setChatId(chatId);
        request.setUserId(1001L);
        request.setTenantId("default");
        request.setRole(ChatRole.EMPLOYEE);
        return doChat(request);
    }

    private void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getChatId() == null || request.getChatId().isBlank()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("role 不能为空");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message 不能为空");
        }
    }

    private String buildRoleSystemPrompt(ChatRequest request) {
        String rolePrompt = switch (request.getRole()) {
            case EMPLOYEE -> """
                    当前用户角色：普通员工
                    你可以帮助处理：
                    - 政策问答
                    - 个人请假 / 加班 / 调休 / 报销 / 证明 / 办公用品申请
                    - 查询“我发起的”或“与我本人相关”的审批进度
                    - 个人数据看板
                    你不得主动暴露他人数据、敏感数据、管理员能力。
                    """;
            case HR -> """
当前用户角色：HR
你可以帮助处理：
- 政策与制度解释
- 入转调离流程指引
- 证明开具与人工复核流转
- 员工事务支持
- HR 可处理范围内的审批查询、审批通过、审批驳回

当用户明确表达以下意图时，你应优先调用审批工具，而不是只做解释：
- “通过ID为1的请求”
- “驳回 12 号审批”
- “查看 approvalId=3 详情”
- “帮我审批这个单子”

处理规则：
1. 用户给出明确 approvalId 且意图是通过/驳回时，直接调用工具执行。
2. 用户未给理由时，comment 可传空字符串。
3. 若用户只是询问详情，调用 getApprovalSummary。
4. 若工具返回无权限或已处理，必须如实告知。
你不得：
- 泄露无关员工的敏感信息
- 绕过系统审批和人工复核
- 直接承诺系统尚未完成的业务结果
""";

            case ADMIN -> """
当前用户角色：管理员
你可以帮助处理：
- 知识库管理
- 预算规则、提醒规则、系统参数说明
- 审计与工具配置相关问题
- 系统配置和知识库重建任务
- 全部审批的查询、通过、驳回

当用户明确表达以下意图时，你应优先调用审批工具，而不是只做说明：
- “通过ID为1的请求”
- “驳回审批单5”
- “查询全部待审批”
- “查看 approvalId=9 详情”

处理规则：
1. 用户给出明确 approvalId 且意图是通过/驳回时，直接调用审批工具。
2. 用户只想看详情时，调用 getApprovalSummary。
3. 若工具已完成审批，直接返回处理结果。
你不得：
- 伪造业务数据
- 绕过后端权限控制
- 直接暴露底层敏感数据表内容
""";
        };

        return BASE_SYSTEM_PROMPT + "\n" + rolePrompt;
    }

    private String buildSafeUserPrompt(ChatRequest request) {
        String currentDate = DateContextUtils.currentDate();
        String currentMonth = DateContextUtils.currentMonth();
        String currentYear = DateContextUtils.currentYear();

        return """
请严格遵守工具调用规则回答用户问题。

当前运行上下文：
- 当前日期：%s
- 当前年份：%s
- 当前月份：%s
- userId: %d
- tenantId: %s
- role: %s

业务参数归一化规则：
- “请假” => LEAVE
- “办公用品” => OFFICE_SUPPLY
- “证明 / 在职证明 / 证明开具” => CERTIFICATE
- “报销” => EXPENSE
- “本月 / 这个月 / 当前月 / 最近” => 当前月份 %s
- “今年4月” => 当前年份的 04 月
- “2026年4月 / 2026-04” => 2026-04
- “最近的历史审批记录”如果没有指定月份，默认查询当前月份 %s

用户问题：
%s
""".formatted(
                currentDate,
                currentYear,
                currentMonth,
                request.getUserId(),
                request.getTenantId() == null ? "default" : request.getTenantId(),
                request.getRole().name(),
                currentMonth,
                currentMonth,
                request.getMessage()
        );
    }

    private ToolCallback[] getToolsByRole(ChatRole role) {
        return switch (role) {
            case EMPLOYEE -> employeeTools;
            case HR -> hrTools;
            case ADMIN -> adminTools;
        };
    }

    private org.springframework.ai.chat.client.advisor.api.Advisor resolveAdvisorByMessage(String message) {
        String text = message == null ? "" : message.toLowerCase();

        if (containsAny(text,
                "政策", "制度", "年假", "育儿假", "病假", "社保", "公积金", "薪资", "福利", "报销标准", "合规", "班车", "放假")) {
            return hrRagAdvisorFactory.createPolicyAdvisor();
        }

        if (containsAny(text,
                "流程", "入职", "离职", "转岗", "交接", "审批级别", "第一天要带什么", "怎么申请")) {
            return hrRagAdvisorFactory.createProcessAdvisor();
        }

        return hrRagAdvisorFactory.createDefaultAdvisor();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}