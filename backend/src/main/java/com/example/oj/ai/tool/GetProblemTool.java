package com.example.oj.ai.tool;

import com.example.oj.ai.HintContext;
import com.example.oj.ai.HintTool;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.entity.Problem;
import com.example.oj.mapper.ProblemMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetProblemTool implements HintTool {

    private final ProblemMapper problemMapper;

    public GetProblemTool(ProblemMapper problemMapper) {
        this.problemMapper = problemMapper;
    }

    @Override
    public String name() {
        return "get_problem";
    }

    @Override
    public String description() {
        return "获取当前题目的标题、题面、时间与内存限制。只返回公开题面信息，不包含测试数据。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.<String, Object>of(
                "type", "object",
                "properties", Map.of("problemId", Map.of("type", "integer", "description", "题目 ID")),
                "required", List.of("problemId"));
    }

    @Override
    public String execute(Map<String, Object> arguments, HintContext context) {
        Long problemId = requireLong(arguments, "problemId");
        if (!problemId.equals(context.problemId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询当前题目的信息");
        }
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return "标题：" + problem.getTitle()
                + "\n难度：" + problem.getDifficulty()
                + "\n时间限制：" + problem.getTimeLimitMs() + "ms"
                + "\n内存限制：" + problem.getMemoryLimitMb() + "MB"
                + "\n题面：\n" + problem.getDescription();
    }

    private Long requireLong(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        if (!(value instanceof Number number)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少参数：" + key);
        }
        return number.longValue();
    }
}