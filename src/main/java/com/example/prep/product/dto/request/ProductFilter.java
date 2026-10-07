package com.example.prep.product.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductFilter(
    @Size(max = 60, message = "category must be at most 60 characters") String category,
    @DecimalMin(value = "0", message = "minPrice must be zero or more") BigDecimal minPrice,
    @DecimalMin(value = "0", message = "maxPrice must be zero or more") BigDecimal maxPrice,
    Boolean inStock,
    @Size(max = 120, message = "q must be at most 120 characters") String q) {

  @AssertTrue(message = "minPrice must not be greater than maxPrice")
  public boolean isPriceRangeValid() {
    return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
  }
}
