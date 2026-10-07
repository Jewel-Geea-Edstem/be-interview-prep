package com.example.prep.order.controller;

import com.example.prep.order.dto.request.OrderRequest;
import com.example.prep.order.dto.response.OrderResponse;
import com.example.prep.order.service.OrderService;
import com.example.prep.order.service.PlacedOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderResponse> place(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(IDEMPOTENCY_KEY)
          @NotBlank(message = "Idempotency-Key must not be blank")
          @Size(max = 100, message = "Idempotency-Key must be at most 100 characters")
          String idempotencyKey,
      @Valid @RequestBody OrderRequest request) {
    PlacedOrder placed = orderService.place(jwt.getSubject(), idempotencyKey, request);
    OrderResponse order = placed.order();
    if (!placed.created()) {
      return ResponseEntity.ok(order);
    }
    return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id) {
    return orderService.get(jwt.getSubject(), id);
  }

  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id) {
    return orderService.cancel(jwt.getSubject(), id);
  }
}
