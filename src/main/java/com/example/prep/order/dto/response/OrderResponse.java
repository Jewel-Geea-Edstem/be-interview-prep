package com.example.prep.order.dto.response;

import com.example.prep.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    OrderStatus status,
    List<OrderItemResponse> items,
    BigDecimal total,
    Instant createdAt) {}
