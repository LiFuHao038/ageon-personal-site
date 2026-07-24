package cn.ageon.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String detail,
        @NotBlank @Size(max = 40) String tag
) {
}
