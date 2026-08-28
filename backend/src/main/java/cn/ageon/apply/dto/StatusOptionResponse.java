package cn.ageon.apply.dto;

import java.util.List;

public record StatusOptionResponse(
        String status,
        String label,
        boolean terminal,
        List<String> allowed
) {
}
