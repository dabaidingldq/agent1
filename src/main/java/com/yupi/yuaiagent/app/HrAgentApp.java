package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.service.ChatSessionService;
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

            你必须遵守以下规则：
            1. 涉及公司政策、员工手册、流程规范时，优先基于知识库内容回答，不允许凭空编造。
            2. 涉及请假、审批、办公用品、证明开具、数据看板等操作时，优先调用工具获取结果。
            3. 对于身份证号、工资明细、住址、联系方式、他人数据、敏感审批信息等内容，不得自行猜测，不得主动扩展。
            4. 如果工具返回“无权限”或“需要人工复核”，你只能如实告知用户，不得绕过限制。
            5. 对于不能直接办理的请求，要明确说明原因，并给出下一步建议。
            6. 回答风格要专业、简洁、面向企业场景，不要使用恋爱、情感、陪聊类话术。
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
                    你不得主动暴露他人数据、团队敏感数据、管理员能力。
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
        return """
                请基于知识库和工具结果回答用户问题。
                当前上下文：
                - userId: %d
                - tenantId: %s
                - role: %s

                用户问题：
                %s
                """.formatted(
                request.getUserId(),
                request.getTenantId() == null ? "default" : request.getTenantId(),
                request.getRole().name(),
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