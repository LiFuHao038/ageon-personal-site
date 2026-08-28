package cn.ageon.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourcePreviewRequest(
        @NotBlank @Size(max = 500) String url
) {
}
