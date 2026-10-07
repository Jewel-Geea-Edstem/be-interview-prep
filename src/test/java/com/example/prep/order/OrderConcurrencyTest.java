package com.example.prep.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prep.common.exception.ConflictException;
import com.example.prep.order.dto.request.OrderItemRequest;
import com.example.prep.order.dto.request.OrderRequest;
import com.example.prep.order.repository.OrderRepository;
import com.example.prep.order.service.OrderService;
import com.example.prep.order.service.PlacedOrder;
import com.example.prep.product.entity.Product;
import com.example.prep.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderConcurrencyTest {

  private static final String CUSTOMER = "alice@example.com";

  @Autowired private OrderService orderService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private ProductRepository productRepository;

  @Test
  void fiftyConcurrentOrdersForTenUnitsSellExactlyTen() throws Exception {
    Long productId = product("Concurrency Widget " + UUID.randomUUID(), 10);
    OrderRequest request = request(productId, 1);

    List<Outcome> outcomes =
        runConcurrently(
            50, () -> orderService.place(CUSTOMER, UUID.randomUUID().toString(), request));

    assertThat(outcomes.stream().filter(Outcome::succeeded)).hasSize(10);
    assertThat(outcomes.stream().filter(Outcome::failed))
        .hasSize(40)
        .allSatisfy(
            outcome ->
                assertThat(outcome.error())
                    .isInstanceOf(ConflictException.class)
                    .hasMessageStartingWith("Insufficient stock for product " + productId));
    assertThat(stock(productId)).isZero();
  }

  @Test
  void concurrentRetriesWithOneKeyCreateOneOrderAndReserveOnce() throws Exception {
    Long productId = product("Retry Widget " + UUID.randomUUID(), 10);
    OrderRequest request = request(productId, 3);
    String key = UUID.randomUUID().toString();

    List<Outcome> outcomes = runConcurrently(10, () -> orderService.place(CUSTOMER, key, request));

    assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failed()).isFalse());
    assertThat(outcomes.stream().map(outcome -> outcome.placed().order().id()).distinct())
        .hasSize(1);
    assertThat(outcomes.stream().filter(outcome -> outcome.placed().created())).hasSize(1);
    assertThat(orderRepository.countByIdempotencyKeyAndCustomerEmail(key, CUSTOMER)).isEqualTo(1);
    assertThat(stock(productId)).isEqualTo(7);
  }

  @Test
  void concurrentCancelsRestoreStockOnce() throws Exception {
    Long productId = product("Cancel Widget " + UUID.randomUUID(), 10);
    Long orderId =
        orderService
            .place(CUSTOMER, UUID.randomUUID().toString(), request(productId, 4))
            .order()
            .id();

    List<Outcome> outcomes =
        runConcurrently(10, () -> new PlacedOrder(orderService.cancel(CUSTOMER, orderId), false));

    assertThat(outcomes.stream().filter(Outcome::succeeded)).hasSize(1);
    assertThat(outcomes.stream().filter(Outcome::failed))
        .hasSize(9)
        .allSatisfy(outcome -> assertThat(outcome.error()).isInstanceOf(ConflictException.class));
    assertThat(stock(productId)).isEqualTo(10);
  }

  private List<Outcome> runConcurrently(int count, Callable<PlacedOrder> call) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(count);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<PlacedOrder>> futures = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return call.call();
                }));
      }
      start.countDown();
      List<Outcome> outcomes = new ArrayList<>();
      for (Future<PlacedOrder> future : futures) {
        try {
          outcomes.add(new Outcome(future.get(60, TimeUnit.SECONDS), null));
        } catch (ExecutionException ex) {
          outcomes.add(new Outcome(null, ex.getCause()));
        }
      }
      return outcomes;
    } finally {
      executor.shutdownNow();
    }
  }

  private Long product(String name, int stock) {
    Product product = new Product();
    product.setName(name.substring(0, Math.min(name.length(), 120)));
    product.setCategory("Test");
    product.setPrice(new BigDecimal("9.99"));
    product.setStock(stock);
    product.setRating(new BigDecimal("4.0"));
    return productRepository.save(product).getId();
  }

  private int stock(Long productId) {
    return productRepository.findById(productId).orElseThrow().getStock();
  }

  private static OrderRequest request(Long productId, int quantity) {
    return new OrderRequest(List.of(new OrderItemRequest(productId, quantity)));
  }

  private record Outcome(PlacedOrder placed, Throwable error) {

    boolean succeeded() {
      return error == null;
    }

    boolean failed() {
      return error != null;
    }
  }
}
