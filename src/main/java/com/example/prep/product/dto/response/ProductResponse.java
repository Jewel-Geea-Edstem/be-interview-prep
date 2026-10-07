package com.example.prep.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String name,
    String category,
    BigDecimal price,
    int stock,
    BigDecimal rating,
    Instant createdAt) {}
