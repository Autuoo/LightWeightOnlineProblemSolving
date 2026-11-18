package com.example.oj.judge.sandbox;

public interface SandboxRunner {

    SandboxResult run(SandboxSpec spec);

    String type();
}