package cn.ageon.apply.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 字段全部为 null 表示不修改；状态不在此处变更，走 POST /{id}/status。
 */
public record UpdateApplicationRequest(
        @Size(max = 60) String company,
        @Size(max = 80) String position,
        @Size(max = 40) String city,
        @Size(max = 20) String companyType,
        @Size(max = 20) String channel,
        @Size(max = 500) String sourceUrl,
        LocalDate deadlineAt,
        LocalDate appliedAt,
        @Size(max = 1000) String note
) {
}
