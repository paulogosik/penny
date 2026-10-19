package com.penny.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhoneNumberRequest(@NotBlank @Size(max = 32) String phoneNumber) {
}
