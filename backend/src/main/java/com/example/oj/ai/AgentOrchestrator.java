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
            6. 学生的问题才是本次回答的核心，必须直接回答它，不要答非所问。
            7. 先判断问题类型（失败诊断 / 复杂度 / 输入输出 / 边界条件），只围绕该问题作答。
            8. 若发现与学生所问无关的问题，先完整回答学生的问题，最后最多用一句话补充，不要放在开头。
            9. 学生问复杂度时，必须基于代码结构给出复杂度量级（如 O(n)、O(n log n)、O(n²)）与优化方向，即使代码仍有错误；错误本身最多一句话带过。
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
                        + "\n学生的问题（本次回答的核心，请直接回答）：" + question
                        + (isPerformanceQuestion(question)
                        ? "\n注意：这是复杂度或性能类问题，必须先给出复杂度量级判断。"
                        : "")
                        + "\n回答顺序：先完整回答学生的问题，再视情况用一句话补充其他风险。"
                        + "\n按问题需要选择工具：诊断失败用 get_failure_summary；"
                        + "分析复杂度或实现用 get_problem、get_my_submission、get_language_limits。"));

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

    private static boolean isPerformanceQuestion(String question) {
        if (question == null) {
            return false;
        }
        return question.contains("复杂度")
                || question.contains("性能")
                || question.contains("优化")
                || question.contains("超时")
                || question.contains("效率");
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
            case 1 -> "只给出 1-2 个方向性结论，不展开具体改写，80 字以内";
            case 2 -> "指出问题所在的具体环节（边界、格式、复杂度、数据类型），可提及测试点序号，150 字以内";
            default -> "给出 3-4 条自查清单，每条一句话，不要给代码，220 字以内";
        };
    }
}