package com.penny.dto;

import jakarta.validation.constraints.Size;

public record MeRequest(
    @Size(max = 100) String name,
    @Size(max = 64) String timezone
) {
}
