package com.example.oj.ai;

import java.util.Map;

public interface HintTool {

    String name();

    String description();

    Map<String, Object> parameters();

    String execute(Map<String, Object> arguments, HintContext context);
}