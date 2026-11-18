package com.example.oj.ai.tool;

import com.example.oj.ai.FailureSummaryService;
import com.example.oj.ai.HintContext;
import com.example.oj.ai.HintTool;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetFailureSummaryTool implements HintTool {

    private final FailureSummaryService failureSummaryService;

    public GetFailureSummaryTool(FailureSummaryService failureSummaryService) {
        this.failureSummaryService = failureSummaryService;
    }

    @Override
    public String name() {
        return "get_failure_summary";
    }

    @Override
    public String description() {
        return "获取本次提交的失败摘要（失败测试点序号、失败类型、输出行数与字符长度、耗时与内存）。"
                + "只返回元信息，绝对不包含测试点输入或期望输出内容。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.<String, Object>of(
                "type", "object",
                "properties", Map.of("submissionId", Map.of("type", "integer", "description", "提交 ID")),
                "required", List.of("submissionId"));
    }

    @Override
    public String execute(Map<String, Object> arguments, HintContext context) {
        Long submissionId = requireLong(arguments, "submissionId");
        if (!submissionId.equals(context.submissionId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询本次提交");
        }
        FailureSummaryService.FailureSummary summary =
                failureSummaryService.summarize(submissionId, context.userId());

        StringBuilder builder = new StringBuilder();
        builder.append("测试点总数：").append(summary.totalCases())
                .append("，已通过：").append(summary.passedCases());
        if (summary.firstFailedOrderNo() == null) {
            return builder.append("，没有失败测试点。").toString();
        }
        builder.append("\n首个失败测试点序号：").append(summary.firstFailedOrderNo())
                .append("，失败类型：").append(summary.firstFailedStatus());
        if (summary.actualChars() != null) {
            builder.append("\n你的输出：").append(summary.actualLines()).append(" 行，")
                    .append(summary.actualChars()).append(" 个字符");
            builder.append("\n期望输出：").append(summary.expectedLines()).append(" 行，")
                    .append(summary.expectedChars()).append(" 个字符");
        }
        if (summary.execTimeMs() != null) {
            builder.append("\n耗时：").append(summary.execTimeMs()).append("ms");
        }
        if (summary.memoryKb() != null) {
            builder.append("\n内存：").append(summary.memoryKb()).append("KB");
        }
        return builder.toString();
    }

    private Long requireLong(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        if (!(value instanceof Number number)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少参数：" + key);
        }
        return number.longValue();
    }
}