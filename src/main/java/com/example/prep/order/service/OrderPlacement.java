package com.example.prep.order.service;

import com.example.prep.common.exception.NotFoundException;
import com.example.prep.order.dto.response.OrderResponse;
import com.example.prep.order.entity.Order;
import com.example.prep.order.entity.OrderItem;
import com.example.prep.order.entity.OrderStatus;
import com.example.prep.order.mapper.OrderMapper;
import com.example.prep.order.repository.OrderRepository;
import com.example.prep.product.entity.Product;
import com.example.prep.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPlacement {

  private final StockService stockService;
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;

  @Transactional
  public OrderResponse place(
      String customerEmail, String idempotencyKey, String requestHash, List<OrderLine> lines) {
    Map<Long, BigDecimal> prices =
        productRepository.findAllById(lines.stream().map(OrderLine::productId).toList()).stream()
            .collect(Collectors.toMap(Product::getId, Product::getPrice));
    Order order = new Order();
    order.setCustomerEmail(customerEmail);
    order.setIdempotencyKey(idempotencyKey);
    order.setRequestHash(requestHash);
    order.setStatus(OrderStatus.PLACED);
    for (OrderLine line : lines) {
      BigDecimal price = prices.get(line.productId());
      if (price == null) {
        throw new NotFoundException("Product", line.productId());
      }
      OrderItem item = new OrderItem();
      item.setProductId(line.productId());
      item.setQuantity(line.quantity());
      item.setUnitPrice(price);
      order.addItem(item);
    }
    Order claimed = orderRepository.saveAndFlush(order);
    stockService.reserve(lines);
    return orderMapper.toResponse(claimed);
  }
}
