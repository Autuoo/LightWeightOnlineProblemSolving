package com.example.oj.ai;

import com.example.oj.ai.llm.LlmClient;
import com.example.oj.ai.llm.LlmMessage;
import com.example.oj.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class AgentOrchestrator {

    private static final String SYSTEM_PROMPT = """
            你是在线判题系统的编程助教，面向正在学习编程的学生。
            规则：
            1. 只能依据工具返回的信息作答，不得编造测试数据。
            2. 不得给出正确答案、完整可提交代码或直接的输出内容。
            3. 用启发式方式指出问题方向，例如输出格式、边界条件、复杂度。
            4. 回答控制在 220 字以内，使用中文。
            5. 严格遵守本次请求指定的提示层级。
            """;

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final AiProperties properties;

    public AgentOrchestrator(LlmClient llmClient, ToolRegistry toolRegistry, AiProperties properties) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    public HintResult orchestrate(HintContext context, String question, int level,
                                  Consumer<HintResult.ToolTrace> onToolCall) {
        int normalized = normalizeLevel(level);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(SYSTEM_PROMPT));
        messages.add(LlmMessage.user(
                "当前提交 ID：" + context.submissionId()
                        + "\n当前题目 ID：" + context.problemId()
                        + "\n提示层级：" + levelName(normalized) + "（" + levelRule(normalized) + "）"
                        + "\n请先调用工具查看这次提交的失败摘要和我的代码，再给出提示。"
                        + "\n学生的问题：" + question));

        List<HintResult.ToolTrace> traces = new ArrayList<>();
        int promptTokens = 0;
        int completionTokens = 0;

        for (int step = 0; step < properties.getMaxSteps(); step++) {
            LlmClient.LlmResponse response = llmClient.chat(messages, toolRegistry.schemas());
            promptTokens += response.promptTokens();
            completionTokens += response.completionTokens();

            if (!response.hasToolCalls()) {
                return new HintResult(response.content(), traces, step + 1,
                        promptTokens, completionTokens, "OK");
            }

            messages.add(LlmMessage.assistant(response.content(), response.toolCalls()));
            for (LlmClient.ToolCall call : response.toolCalls()) {
                if (!toolRegistry.isRegistered(call.name())) {
                    messages.add(LlmMessage.tool(call.id(), "工具不存在"));
                    continue;
                }
                ToolRegistry.ToolExecution execution = toolRegistry.execute(call, context);
                HintResult.ToolTrace trace = new HintResult.ToolTrace(
                        execution.name(), execution.ok(), execution.elapsedMs());
                traces.add(trace);
                if (onToolCall != null) {
                    onToolCall.accept(trace);
                }
                messages.add(LlmMessage.tool(call.id(), execution.output()));
            }
        }

        return new HintResult("抱歉，我需要更多信息才能给出提示，请补充你的思路后再试。",
                traces, properties.getMaxSteps(), promptTokens, completionTokens, "STEP_LIMIT");
    }

    public static int normalizeLevel(int level) {
        return Math.max(1, Math.min(3, level));
    }

    private static String levelName(int level) {
        return switch (level) {
            case 1 -> "思路";
            case 2 -> "定位";
            default -> "检查点";
        };
    }

    private static String levelRule(int level) {
        return switch (level) {
            case 1 -> "只指出 1-2 个可能的方向，不要定位到具体语句，80 字以内";
            case 2 -> "指出最可能出错的环节，如输入边界、输出格式、循环边界、数据类型、复杂度，可提及测试点序号，150 字以内";
            default -> "给出 3-4 条自查清单，每条一句话，不要给代码，220 字以内";
        };
    }
}