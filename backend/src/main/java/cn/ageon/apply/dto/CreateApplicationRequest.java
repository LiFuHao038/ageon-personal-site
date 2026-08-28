package cn.ageon.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 60) String company,
        @NotBlank @Size(max = 80) String position,
        @Size(max = 40) String city,
        @Size(max = 20) String companyType,
        @Size(max = 20) String channel,
        @Size(max = 500) String sourceUrl,
        LocalDate deadlineAt,
        @NotNull LocalDate appliedAt,
        @Size(max = 1000) String note
) {
}
