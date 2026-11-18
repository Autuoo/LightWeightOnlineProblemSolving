package com.example.oj.common;

public enum ErrorCode {

    BAD_REQUEST(400, 40001, "参数校验失败"),
    UNSUPPORTED_LANGUAGE(400, 40002, "不支持的语言"),
    DUPLICATE_CASE_ORDER(400, 40003, "测试点序号重复"),

    LOGIN_FAILED(401, 40101, "用户名或密码错误"),
    TOKEN_EXPIRED(401, 40102, "登录已过期"),
    INVALID_TOKEN(401, 40103, "无效凭证"),

    FORBIDDEN(403, 40301, "无权限"),
    NOT_FOUND(404, 40401, "资源不存在"),

    JUDGE_QUEUE_FULL(429, 42901, "评测队列已满，请稍后重试"),
    RATE_LIMITED(429, 42902, "操作频率超限"),
    AI_QUOTA_EXCEEDED(429, 42903, "AI 助教今日额度已用完"),

    INTERNAL_ERROR(500, 50001, "系统内部错误"),
    SANDBOX_UNAVAILABLE(500, 50002, "沙箱不可用"),
    JUDGE_TIMEOUT(500, 50003, "评测超时未收敛"),
    AI_UNAVAILABLE(500, 50004, "AI 助教模型不可用"),
    AI_STEP_LIMIT(500, 50005, "AI 助教达到步数上限");

    private final int httpStatus;
    private final int code;
    private final String message;

    ErrorCode(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}