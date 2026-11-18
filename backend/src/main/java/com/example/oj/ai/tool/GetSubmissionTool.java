package com.example.oj.ai.tool;

import com.example.oj.ai.HintContext;
import com.example.oj.ai.HintTool;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.entity.Submission;
import com.example.oj.mapper.SubmissionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetSubmissionTool implements HintTool {

    private static final int CODE_LIMIT = 4000;

    private final SubmissionMapper submissionMapper;

    public GetSubmissionTool(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    @Override
    public String name() {
        return "get_my_submission";
    }

    @Override
    public String description() {
        return "获取学生自己本次提交的代码、语言、状态与得分。只能查询本次提交。";
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
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询自己的提交");
        }
        String code = submission.getCode() == null ? "" : submission.getCode();
        if (code.length() > CODE_LIMIT) {
            code = code.substring(0, CODE_LIMIT) + "\n...[代码已截断]";
        }
        return "语言：" + submission.getLanguage()
                + "\n状态：" + submission.getStatus()
                + "\n得分：" + submission.getScore()
                + "\n代码：\n" + code;
    }

    private Long requireLong(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        if (!(value instanceof Number number)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少参数：" + key);
        }
        return number.longValue();
    }
}