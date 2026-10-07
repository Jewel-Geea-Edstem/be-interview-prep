package com.example.prep.order.repository;

import com.example.prep.order.entity.Order;
import com.example.prep.order.entity.OrderStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = "items")
  Optional<Order> findByIdAndCustomerEmail(Long id, String customerEmail);

  @EntityGraph(attributePaths = "items")
  Optional<Order> findByIdempotencyKeyAndCustomerEmail(String idempotencyKey, String customerEmail);

  long countByIdempotencyKeyAndCustomerEmail(String idempotencyKey, String customerEmail);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Order o set o.status = :to where o.id = :id and o.status = :from")
  int transition(
      @Param("id") Long id, @Param("from") OrderStatus from, @Param("to") OrderStatus to);
}
