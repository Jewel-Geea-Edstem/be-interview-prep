package com.example.prep.order.mapper;

import com.example.prep.order.dto.response.OrderItemResponse;
import com.example.prep.order.dto.response.OrderResponse;
import com.example.prep.order.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

  public OrderResponse toResponse(Order order) {
    List<OrderItemResponse> items =
        order.getItems().stream()
            .map(
                item ->
                    new OrderItemResponse(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
            .toList();
    BigDecimal total =
        items.stream().map(OrderItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new OrderResponse(order.getId(), order.getStatus(), items, total, order.getCreatedAt());
  }
}
