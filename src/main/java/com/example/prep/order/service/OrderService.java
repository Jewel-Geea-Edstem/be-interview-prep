package com.example.prep.order.service;

import com.example.prep.common.exception.ConflictException;
import com.example.prep.common.exception.NotFoundException;
import com.example.prep.order.dto.request.OrderRequest;
import com.example.prep.order.dto.response.OrderResponse;
import com.example.prep.order.entity.Order;
import com.example.prep.order.entity.OrderStatus;
import com.example.prep.order.mapper.OrderMapper;
import com.example.prep.order.repository.OrderRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderPlacement orderPlacement;
  private final StockService stockService;
  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;

  public PlacedOrder place(String customerEmail, String idempotencyKey, OrderRequest request) {
    List<OrderLine> lines = OrderLine.canonical(request.items());
    String requestHash = OrderLine.hash(lines);
    Optional<OrderResponse> replay = replay(customerEmail, idempotencyKey, requestHash);
    if (replay.isPresent()) {
      return new PlacedOrder(replay.get(), false);
    }
    try {
      return new PlacedOrder(
          orderPlacement.place(customerEmail, idempotencyKey, requestHash, lines), true);
    } catch (DataIntegrityViolationException ex) {
      if (!isIdempotencyKeyClash(ex)) {
        throw ex;
      }
      log.info("Concurrent retry detected for idempotency key {}", idempotencyKey);
      return replay(customerEmail, idempotencyKey, requestHash)
          .map(existing -> new PlacedOrder(existing, false))
          .orElseThrow(
              () ->
                  new ConflictException(
                      "A request with this Idempotency-Key is still being processed"));
    }
  }

  @Transactional(readOnly = true)
  public OrderResponse get(String customerEmail, Long id) {
    return orderMapper.toResponse(find(customerEmail, id));
  }

  @Transactional
  public OrderResponse cancel(String customerEmail, Long id) {
    Order order = find(customerEmail, id);
    List<OrderLine> lines =
        order.getItems().stream()
            .map(item -> new OrderLine(item.getProductId(), item.getQuantity()))
            .toList();
    if (orderRepository.transition(id, OrderStatus.PLACED, OrderStatus.CANCELLED) == 0) {
      throw new ConflictException("Order " + id + " is already cancelled");
    }
    stockService.release(lines);
    return orderMapper.toResponse(find(customerEmail, id));
  }

  private Optional<OrderResponse> replay(
      String customerEmail, String idempotencyKey, String requestHash) {
    return orderRepository
        .findByIdempotencyKeyAndCustomerEmail(idempotencyKey, customerEmail)
        .map(
            existing -> {
              if (!existing.getRequestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency-Key reused with a different request");
              }
              return orderMapper.toResponse(existing);
            });
  }

  private static boolean isIdempotencyKeyClash(DataIntegrityViolationException ex) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation
          && violation.getConstraintName() != null
          && violation
              .getConstraintName()
              .toLowerCase(Locale.ROOT)
              .contains(Order.IDEMPOTENCY_KEY_CONSTRAINT)) {
        return true;
      }
    }
    return false;
  }

  private Order find(String customerEmail, Long id) {
    return orderRepository
        .findByIdAndCustomerEmail(id, customerEmail)
        .orElseThrow(() -> new NotFoundException("Order", id));
  }
}
