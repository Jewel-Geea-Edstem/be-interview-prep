package com.example.prep.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prep.order.repository.OrderRepository;
import com.example.prep.product.entity.Product;
import com.example.prep.product.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

  private static final String ALICE = "alice@example.com";
  private static final String BOB = "bob@example.com";

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;

  @Test
  void placesOrderWithLocationAndReservesStock() throws Exception {
    Long productId = product("Order Lamp", "12.50", 5);

    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), items(productId, 2)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith("/api/v1/orders/")))
        .andExpect(jsonPath("$.status").value("PLACED"))
        .andExpect(jsonPath("$.items[0].productId").value(productId))
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.items[0].unitPrice").value(12.5))
        .andExpect(jsonPath("$.total").value(25.0));

    assertThat(stock(productId)).isEqualTo(3);
  }

  @Test
  void duplicateProductLinesAreMerged() throws Exception {
    Long productId = product("Order Merge", "1.00", 5);
    String body =
        """
        {"items":[{"productId":%d,"quantity":1},{"productId":%d,"quantity":2}]}
        """
            .formatted(productId, productId);

    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].quantity").value(3));

    assertThat(stock(productId)).isEqualTo(2);
  }

  @Test
  void retryWithSameKeyReturnsTheSameOrderOnce() throws Exception {
    Long productId = product("Order Retry", "3.00", 10);
    String key = UUID.randomUUID().toString();

    Long firstId =
        idOf(
            mockMvc
                .perform(order(as(ALICE), key, items(productId, 4)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    Long secondId =
        idOf(
            mockMvc
                .perform(order(as(ALICE), key, items(productId, 4)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(secondId).isEqualTo(firstId);
    assertThat(orderRepository.countByIdempotencyKeyAndCustomerEmail(key, ALICE)).isEqualTo(1);
    assertThat(stock(productId)).isEqualTo(6);
  }

  @Test
  void sameKeyWithDifferentBodyIsRejected() throws Exception {
    Long productId = product("Order Key Reuse", "3.00", 10);
    String key = UUID.randomUUID().toString();
    mockMvc.perform(order(as(ALICE), key, items(productId, 1))).andExpect(status().isCreated());

    mockMvc
        .perform(order(as(ALICE), key, items(productId, 2)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Idempotency-Key reused with a different request"));

    assertThat(stock(productId)).isEqualTo(9);
  }

  @Test
  void sameKeyFromAnotherCustomerIsAnIndependentOrder() throws Exception {
    Long productId = product("Order Key Scope", "3.00", 10);
    String key = UUID.randomUUID().toString();

    mockMvc.perform(order(as(ALICE), key, items(productId, 1))).andExpect(status().isCreated());
    mockMvc.perform(order(as(BOB), key, items(productId, 1))).andExpect(status().isCreated());

    assertThat(stock(productId)).isEqualTo(8);
  }

  @Test
  void orderIsAllOrNothingWhenOneItemIsShort() throws Exception {
    Long available = product("Order Plenty", "2.00", 10);
    Long scarce = product("Order Scarce", "2.00", 1);
    String body =
        """
        {"items":[{"productId":%d,"quantity":2},{"productId":%d,"quantity":3}]}
        """
            .formatted(available, scarce);

    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), body))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Insufficient stock for product %d: requested 3, available 1"
                        .formatted(scarce)));

    assertThat(stock(available)).isEqualTo(10);
    assertThat(stock(scarce)).isEqualTo(1);
  }

  @Test
  void unknownProductReturnsNotFoundAndReservesNothing() throws Exception {
    Long productId = product("Order Before Missing", "2.00", 4);
    String body =
        """
        {"items":[{"productId":%d,"quantity":1},{"productId":999999,"quantity":1}]}
        """
            .formatted(productId);

    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product 999999 not found"));

    assertThat(stock(productId)).isEqualTo(4);
  }

  @Test
  void cancelRestoresStockAndSecondCancelConflicts() throws Exception {
    Long productId = product("Order Cancel", "5.00", 5);
    Long orderId = place(ALICE, productId, 3);
    assertThat(stock(productId)).isEqualTo(2);

    mockMvc
        .perform(post("/api/v1/orders/{id}/cancel", orderId).with(as(ALICE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(stock(productId)).isEqualTo(5);

    mockMvc
        .perform(post("/api/v1/orders/{id}/cancel", orderId).with(as(ALICE)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Order " + orderId + " is already cancelled"));
    assertThat(stock(productId)).isEqualTo(5);
  }

  @Test
  void ownerCanReadOrderButOthersGetNotFound() throws Exception {
    Long orderId = place(ALICE, product("Order Private", "5.00", 5), 1);

    mockMvc
        .perform(get("/api/v1/orders/{id}", orderId).with(as(ALICE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId));
    mockMvc
        .perform(get("/api/v1/orders/{id}", orderId).with(as(BOB)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(post("/api/v1/orders/{id}/cancel", orderId).with(as(BOB)))
        .andExpect(status().isNotFound());
  }

  @Test
  void missingOrBlankIdempotencyKeyIsBadRequest() throws Exception {
    Long productId = product("Order No Key", "5.00", 5);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(as(ALICE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(productId, 1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Missing required header 'Idempotency-Key'"));
    mockMvc.perform(order(as(ALICE), " ", items(productId, 1))).andExpect(status().isBadRequest());
    mockMvc
        .perform(order(as(ALICE), "k".repeat(101), items(productId, 1)))
        .andExpect(status().isBadRequest());

    assertThat(stock(productId)).isEqualTo(5);
  }

  @Test
  void invalidItemsAreBadRequest() throws Exception {
    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), "{\"items\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("items"));
    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), items(1L, 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("items[0].quantity"));
    mockMvc
        .perform(order(as(ALICE), UUID.randomUUID().toString(), items(1L, 1001)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void requestsWithoutTokenAreUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(1L, 1)))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/orders/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void productReadShowsReducedStockAfterOrder() throws Exception {
    Long productId = product("Order Cache", "5.00", 8);
    mockMvc
        .perform(get("/api/v1/products/{id}", productId).with(as(ALICE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(8));

    Long orderId = place(ALICE, productId, 3);
    mockMvc
        .perform(get("/api/v1/products/{id}", productId).with(as(ALICE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(5));

    mockMvc
        .perform(post("/api/v1/orders/{id}/cancel", orderId).with(as(ALICE)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/products/{id}", productId).with(as(ALICE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(8));
  }

  private Long place(String customer, Long productId, int quantity) throws Exception {
    return idOf(
        mockMvc
            .perform(order(as(customer), UUID.randomUUID().toString(), items(productId, quantity)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private static MockHttpServletRequestBuilder order(
      RequestPostProcessor auth, String key, String body) {
    return post("/api/v1/orders")
        .with(auth)
        .header("Idempotency-Key", key)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private static JwtRequestPostProcessor as(String email) {
    return jwt().jwt(j -> j.subject(email));
  }

  private static String items(Long productId, int quantity) {
    return """
        {"items":[{"productId":%d,"quantity":%d}]}
        """
        .formatted(productId, quantity);
  }

  private static Long idOf(String json) {
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  private Long product(String name, String price, int stock) {
    Product product = new Product();
    product.setName(name + " " + UUID.randomUUID());
    product.setCategory("Test");
    product.setPrice(new BigDecimal(price));
    product.setStock(stock);
    product.setRating(new BigDecimal("4.0"));
    return productRepository.save(product).getId();
  }

  private int stock(Long productId) {
    return productRepository.findById(productId).orElseThrow().getStock();
  }
}
