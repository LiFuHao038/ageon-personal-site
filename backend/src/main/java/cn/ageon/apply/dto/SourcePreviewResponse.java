package cn.ageon.apply.dto;

public record SourcePreviewResponse(
        String url,
        String title,
        String logoUrl,
        String error
) {
}
