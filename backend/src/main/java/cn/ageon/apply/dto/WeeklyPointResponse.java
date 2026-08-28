package cn.ageon.apply.dto;

import java.time.LocalDate;

public record WeeklyPointResponse(LocalDate weekStart, int applied) {
}