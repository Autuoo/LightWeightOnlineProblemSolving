package com.example.oj.judge.sandbox;

public record SandboxResult(
        int exitCode,
        boolean timedOut,
        boolean compileFailed,
        String compileOutput,
        long elapsedMs,
        long peakMemoryKb,
        String stdout,
        String stderr,
        boolean oomKilled) {

    public static SandboxResult compileFailed(String output, long elapsedMs) {
        return new SandboxResult(-1, false, true, output, elapsedMs, 0, "", "", false);
    }

    public static SandboxResult timeout(long elapsedMs) {
        return new SandboxResult(-1, true, false, "", elapsedMs, 0, "", "", false);
    }
}