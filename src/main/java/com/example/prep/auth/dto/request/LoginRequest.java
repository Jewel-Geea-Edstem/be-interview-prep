package com.example.prep.auth.dto.request;

import com.example.prep.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "email is required") String email,
    @NotBlank(message = "password is required")
        @MaxUtf8Bytes(value = 72, message = "password must be at most 72 bytes in UTF-8")
        String password) {}
