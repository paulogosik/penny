package com.penny.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhotoExpenseRequest(
    @NotBlank @Size(max = 14_000_000) String imageBase64,
    @NotBlank @Pattern(regexp = "image/(jpeg|png|webp|heic|heif)") String mimeType
) {
}
