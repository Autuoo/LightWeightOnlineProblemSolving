package com.example.oj.dto;

import java.time.LocalDate;
import java.util.List;

public record UserStats(Today today, List<DayPoint> trend) {

    public record Today(long submissions, long accepted, double acceptanceRate, long solvedProblems) {
    }

    public record DayPoint(LocalDate date, long submissions, long accepted, long solvedProblems) {
    }
}