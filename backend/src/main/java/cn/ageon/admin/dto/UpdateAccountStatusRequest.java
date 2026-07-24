package cn.ageon.admin.dto;

import cn.ageon.auth.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(@NotNull AccountStatus status) {
}
