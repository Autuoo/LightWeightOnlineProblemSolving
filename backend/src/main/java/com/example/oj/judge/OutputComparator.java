package com.example.oj.judge;

import org.springframework.stereotype.Component;

@Component
public class OutputComparator {

    public boolean matches(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = unified.split("\n", -1);
        int end = lines.length;
        while (end > 0 && lines[end - 1].isBlank()) {
            end--;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines[i].stripTrailing());
        }
        return builder.toString();
    }
}