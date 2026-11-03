package com.penny.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextMessageRequest(@NotBlank @Size(max = 1000) String text) {
}
