package com.example.prep.url.dto.request;

import com.example.prep.url.validation.ValidUrl;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ShortenRequest(
    @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        @ValidUrl
        String url,
    @Future(message = "expiresAt must be in the future") Instant expiresAt) {}
