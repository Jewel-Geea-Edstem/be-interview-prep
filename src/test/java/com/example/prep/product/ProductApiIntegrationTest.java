package com.example.prep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void seedsOneHundredProductsWithPageMetadata() throws Exception {
    mockMvc
        .perform(get("/api/v1/products").with(jwt()).param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(20))
        .andExpect(jsonPath("$.page.totalElements").value(100))
        .andExpect(jsonPath("$.page.totalPages").value(5))
        .andExpect(jsonPath("$.content[0].createdAt").exists());
  }

  @Test
  void pageSizeIsCappedAtOneHundred() throws Exception {
    mockMvc
        .perform(get("/api/v1/products").with(jwt()).param("size", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.size").value(100));
  }

  @Test
  void combinedFiltersApplyToEveryItem() throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/v1/products")
                    .with(jwt())
                    .param("category", "electronics")
                    .param("minPrice", "50")
                    .param("maxPrice", "400")
                    .param("inStock", "true")
                    .param("q", "PHONE")
                    .param("size", "100"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Map<String, Object>> items = JsonPath.read(body, "$.content");

    assertThat(items).isNotEmpty();
    assertThat(items)
        .allSatisfy(
            item -> {
              BigDecimal price = new BigDecimal(item.get("price").toString());
              assertThat(item.get("category")).isEqualTo("Electronics");
              assertThat(price).isBetween(new BigDecimal("50"), new BigDecimal("400"));
              assertThat((Integer) item.get("stock")).isPositive();
              assertThat(item.get("name").toString().toLowerCase()).contains("phone");
            });
  }

  @Test
  void sortsByPriceDescending() throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/v1/products")
                    .with(jwt())
                    .param("sort", "price,desc")
                    .param("size", "100"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Object> raw = JsonPath.read(body, "$.content[*].price");
    List<BigDecimal> prices = raw.stream().map(p -> new BigDecimal(p.toString())).toList();

    assertThat(prices).hasSize(100).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  void unknownSortPropertyIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/products").with(jwt()).param("sort", "password,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/v1/products"));
  }

  @Test
  void invertedPriceRangeIsRejected() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/products").with(jwt()).param("minPrice", "100").param("maxPrice", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details[0].field").value("priceRangeValid"));
  }

  @Test
  void negativePriceIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/products").with(jwt()).param("minPrice", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("minPrice"));
  }

  @Test
  void unknownProductReturns404() throws Exception {
    mockMvc
        .perform(get("/api/v1/products/{id}", 999999).with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product 999999 not found"));
  }

  @Test
  void repeatedLookupsShowAsCacheHitsInMetrics() throws Exception {
    mockMvc.perform(get("/api/v1/products/{id}", 1).with(jwt())).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/products/{id}", 1).with(jwt())).andExpect(status().isOk());

    String body =
        mockMvc
            .perform(
                get("/actuator/metrics/cache.gets")
                    .with(admin())
                    .param("tag", "cache:products")
                    .param("tag", "result:hit"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Double hits = JsonPath.read(body, "$.measurements[0].value");

    assertThat(hits).isGreaterThanOrEqualTo(1.0);
  }

  @Test
  void plainUserCannotReadActuatorMetrics() throws Exception {
    mockMvc
        .perform(get("/actuator/metrics").with(jwt()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mockMvc
        .perform(delete("/actuator/caches").with(jwt()))
        .andExpect(status().isForbidden());
  }

  @Test
  void productLifecycle() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/v1/products")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(productJson("Desk Lamp", "19.99", 5)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(created, "$.id");

    mockMvc
        .perform(
            put("/api/v1/products/{id}", id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Desk Lamp", "24.50", 3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(24.5));
    mockMvc
        .perform(delete("/api/v1/products/{id}", id).with(admin()))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/products/{id}", id).with(jwt())).andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/api/v1/products/{id}", id).with(admin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void invalidProductReturnsFieldErrors() throws Exception {
    String body =
        """
        {"name":"","category":"Home","price":-1,"stock":-2,"rating":5.5}
        """;

    mockMvc
        .perform(
            post("/api/v1/products")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[?(@.field == 'name')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'price')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'stock')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'rating')]").exists());
  }

  @Test
  void plainUserCannotWriteProducts() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/products")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Desk Lamp", "19.99", 5)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.path").value("/api/v1/products"));
    mockMvc
        .perform(delete("/api/v1/products/{id}", 1).with(jwt()))
        .andExpect(status().isForbidden());
  }

  private JwtRequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private String productJson(String name, String price, int stock) {
    return """
        {"name":"%s","category":"Home","price":%s,"stock":%d,"rating":4.2}
        """
        .formatted(name, price, stock);
  }
}
