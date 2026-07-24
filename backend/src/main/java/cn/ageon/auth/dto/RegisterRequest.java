package cn.ageon.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 40) String displayName,
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+") String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @AssertTrue boolean acceptedTerms
) {
}
