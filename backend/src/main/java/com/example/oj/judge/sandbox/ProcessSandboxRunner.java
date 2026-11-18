package com.example.oj.judge.sandbox;

import com.example.oj.config.SandboxProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "oj.sandbox.type", havingValue = "process", matchIfMissing = true)
public class ProcessSandboxRunner implements SandboxRunner {

    private static final long COMPILE_TIMEOUT_MS = 20_000;

    private final SandboxProperties properties;
    private final boolean windows;

    public ProcessSandboxRunner(SandboxProperties properties) {
        this.properties = properties;
        this.windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public SandboxResult run(SandboxSpec spec) {
        Path dir = null;
        try {
            Path workRoot = Paths.get(properties.getWorkDir());
            Files.createDirectories(workRoot);
            dir = Files.createTempDirectory(workRoot, "run-");
            Files.writeString(dir.resolve(sourceFileName(spec.language())),
                    spec.sourceCode(), StandardCharsets.UTF_8);

            if (!"PYTHON3".equals(spec.language())) {
                SandboxResult compile = compile(dir, spec.language());
                if (compile.exitCode() != 0 || compile.timedOut()) {
                    return compile;
                }
            }
            return execute(dir, spec);
        } catch (IOException ex) {
            return new SandboxResult(-1, false, false, "", 0, 0, "", ex.getMessage(), false);
        } finally {
            deleteQuietly(dir);
        }
    }

    @Override
    public String type() {
        return "process";
    }

    private SandboxResult compile(Path dir, String language) throws IOException {
        List<String> command = compileCommand(language);
        Path stdout = dir.resolve("compile.out");
        Path stderr = dir.resolve("compile.err");

        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile());
        builder.redirectOutput(stdout.toFile());
        builder.redirectError(stderr.toFile());

        long start = System.currentTimeMillis();
        Process process = builder.start();
        boolean finished = waitFor(process, COMPILE_TIMEOUT_MS);
        long elapsed = System.currentTimeMillis() - start;
        if (!finished) {
            process.destroyForcibly();
            return new SandboxResult(-1, true, true, "编译超时", elapsed, 0, "", "", false);
        }
        String output = readQuietly(stdout) + readQuietly(stderr);
        return new SandboxResult(process.exitValue(), false, false, truncate(output), elapsed, 0, "", "", false);
    }

    private SandboxResult execute(Path dir, SandboxSpec spec) throws IOException {
        Path stdin = dir.resolve("input.txt");
        Path stdout = dir.resolve("stdout.txt");
        Path stderr = dir.resolve("stderr.txt");
        Files.writeString(stdin, spec.stdin() == null ? "" : spec.stdin(), StandardCharsets.UTF_8);

        ProcessBuilder builder = new ProcessBuilder(runCommand(spec.language())).directory(dir.toFile());
        builder.redirectInput(stdin.toFile());
        builder.redirectOutput(stdout.toFile());
        builder.redirectError(stderr.toFile());

        long start = System.currentTimeMillis();
        Process process = builder.start();
        boolean finished = waitFor(process, spec.timeLimitMs());
        long elapsed = System.currentTimeMillis() - start;

        if (!finished) {
            process.destroyForcibly();
            waitFor(process, 2000);
            return SandboxResult.timeout(elapsed);
        }

        return new SandboxResult(
                process.exitValue(),
                false,
                false,
                "",
                elapsed,
                0,
                readQuietly(stdout),
                readQuietly(stderr),
                false);
    }

    private boolean waitFor(Process process, long timeoutMs) {
        try {
            return process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return false;
        }
    }

    private List<String> compileCommand(String language) {
        return switch (language) {
            case "CPP17" -> List.of("g++", "-O2", "-std=c++17", "-o", executableName(), "main.cpp");
            case "JAVA17" -> List.of("javac", "-encoding", "UTF-8", "Main.java");
            default -> List.of();
        };
    }

    private List<String> runCommand(String language) {
        return switch (language) {
            case "PYTHON3" -> List.of("python", "main.py");
            case "CPP17" -> List.of(windows ? executableName() : "./" + executableName());
            case "JAVA17" -> List.of("java", "-cp", ".", "Main");
            default -> List.of();
        };
    }

    private String sourceFileName(String language) {
        return switch (language) {
            case "CPP17" -> "main.cpp";
            case "JAVA17" -> "Main.java";
            default -> "main.py";
        };
    }

    private String executableName() {
        return windows ? "main.exe" : "main";
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        int limit = 8192;
        return text.length() <= limit ? text : text.substring(0, limit) + "\n...[truncated]";
    }

    private String readQuietly(Path path) {
        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        } catch (IOException ex) {
            return "";
        }
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}