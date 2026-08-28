package cn.ageon.apply.dto;

import cn.ageon.apply.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ChangeStatusRequest(
        @NotNull ApplicationStatus status,
        Instant occurredAt,
        @Size(max = 300) String note
) {
}
