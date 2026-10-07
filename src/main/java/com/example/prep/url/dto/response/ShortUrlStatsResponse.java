package com.example.prep.url.dto.response;

import java.time.Instant;

public record ShortUrlStatsResponse(
    String code,
    String shortUrl,
    String originalUrl,
    long visitCount,
    Instant createdAt,
    Instant expiresAt) {}
