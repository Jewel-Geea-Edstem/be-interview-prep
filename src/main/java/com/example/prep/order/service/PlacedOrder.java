package com.example.prep.order.service;

import com.example.prep.order.dto.response.OrderResponse;

public record PlacedOrder(OrderResponse order, boolean created) {}
