package cn.ageon.admin.dto;

import cn.ageon.community.ModerationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateModerationRequest(@NotNull ModerationStatus moderationStatus) {
}
