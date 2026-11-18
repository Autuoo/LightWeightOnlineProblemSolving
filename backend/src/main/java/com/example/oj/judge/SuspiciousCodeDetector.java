package com.example.oj.judge;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SuspiciousCodeDetector {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern HASH_COMMENT = Pattern.compile("#[^\\n]*");
    private static final Pattern IF = Pattern.compile("\\bif\\b");
    private static final Pattern ELIF = Pattern.compile("\\belif\\b|\\belse\\s+if\\b");
    private static final Pattern SWITCH = Pattern.compile("\\bswitch\\b");
    private static final Pattern CASE = Pattern.compile("\\bcase\\b");
    private static final Pattern STRING_COMPARE_BRANCH =
            Pattern.compile("\\b(if|elif)\\b[^\\n;{]*==\\s*[\"']|\\belse\\s+if\\b[^\\n;{]*==\\s*[\"']");

    private static final int STRING_COMPARE_THRESHOLD = 2;
    private static final int ELIF_THRESHOLD = 3;
    private static final int BRANCH_THRESHOLD = 8;
    private static final int LINE_LIMIT = 60;

    public Result detect(String language, String code) {
        if (code == null || code.isBlank()) {
            return Result.clean();
        }
        String lang = language == null ? "" : language.toUpperCase(Locale.ROOT);
        String stripped = stripComments(code, lang);
        if (stripped.isBlank()) {
            return Result.clean();
        }

        boolean readsInput = readsInput(lang, stripped);
        boolean writesOutput = writesOutput(lang, stripped);
        int branches = countBranches(stripped);
        int elifCount = count(ELIF, stripped);
        int stringCompare = count(STRING_COMPARE_BRANCH, stripped);
        int lines = meaningfulLines(stripped);

        if (writesOutput && !readsInput) {
            return new Result(true, "未读取任何输入却直接输出，疑似固定答案");
        }
        if (stringCompare >= STRING_COMPARE_THRESHOLD && branches >= 3) {
            return new Result(true,
                    "按输入字符串分支并输出常量（" + stringCompare + " 处），疑似逐用例硬编码");
        }
        if (elifCount >= ELIF_THRESHOLD) {
            return new Result(true, "多级条件分支（" + elifCount + " 处），疑似打表");
        }
        if (branches >= BRANCH_THRESHOLD && lines <= LINE_LIMIT) {
            return new Result(true, "分支语句异常密集（" + branches + " 处），疑似逐用例硬编码");
        }
        return Result.clean();
    }

    private static String stripComments(String code, String lang) {
        String result = BLOCK_COMMENT.matcher(code).replaceAll(" ");
        result = LINE_COMMENT.matcher(result).replaceAll(" ");
        if ("PYTHON3".equals(lang)) {
            result = HASH_COMMENT.matcher(result).replaceAll(" ");
        }
        return result;
    }

    private static boolean readsInput(String lang, String code) {
        return switch (lang) {
            case "PYTHON3" -> containsAny(code, "input(", "sys.stdin", "open(0", "fileinput");
            case "CPP17" -> containsAny(code, "cin", "scanf", "getline", "gets(", "fgets", "fread");
            case "JAVA17" -> containsAny(code, "Scanner", "System.in", "BufferedReader",
                    "readLine(", "readAllBytes");
            default -> containsAny(code, "input(", "cin", "scanf", "Scanner", "System.in");
        };
    }

    private static boolean writesOutput(String lang, String code) {
        return switch (lang) {
            case "PYTHON3" -> containsAny(code, "print(", "sys.stdout", "write(");
            case "CPP17" -> containsAny(code, "cout", "printf(", "puts(", "fwrite(");
            case "JAVA17" -> containsAny(code, "System.out", "printf(", "print(", "println(");
            default -> containsAny(code, "print(", "cout", "printf", "System.out");
        };
    }

    private static int countBranches(String code) {
        return count(IF, code) + count(ELIF, code) + count(SWITCH, code) + count(CASE, code);
    }

    private static int meaningfulLines(String code) {
        return (int) code.lines().filter(line -> !line.isBlank()).count();
    }

    private static boolean containsAny(String code, String... needles) {
        for (String needle : needles) {
            if (code.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public record Result(boolean suspicious, String reason) {
        public static Result clean() {
            return new Result(false, null);
        }
    }
}