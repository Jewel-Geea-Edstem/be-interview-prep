package com.example.prep.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "orders",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_orders_customer_idempotency_key",
            columnNames = {"customer_email", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_email", nullable = false, length = 254)
  private String customerEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(name = "idempotency_key", nullable = false, length = 100)
  private String idempotencyKey;

  @Column(nullable = false, length = 64)
  private String requestHash;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("productId")
  private List<OrderItem> items = new ArrayList<>();

  public void addItem(OrderItem item) {
    item.setOrder(this);
    items.add(item);
  }
}
