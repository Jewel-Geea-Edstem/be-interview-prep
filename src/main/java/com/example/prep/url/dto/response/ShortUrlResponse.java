package com.example.prep.url.dto.response;

import java.time.Instant;

public record ShortUrlResponse(
    String code, String shortUrl, String originalUrl, Instant expiresAt, Instant createdAt) {}
