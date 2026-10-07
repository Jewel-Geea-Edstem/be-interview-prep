package com.example.prep.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
