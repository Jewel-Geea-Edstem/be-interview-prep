package com.example.prep.product.repository;

import com.example.prep.product.dto.request.ProductFilter;
import com.example.prep.product.entity.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

  private ProductSpecifications() {}

  public static Specification<Product> from(ProductFilter filter) {
    List<Specification<Product>> specs = new ArrayList<>();
    if (hasText(filter.category())) {
      specs.add(categoryEquals(filter.category()));
    }
    if (filter.minPrice() != null) {
      specs.add(priceAtLeast(filter.minPrice()));
    }
    if (filter.maxPrice() != null) {
      specs.add(priceAtMost(filter.maxPrice()));
    }
    if (Boolean.TRUE.equals(filter.inStock())) {
      specs.add(inStock());
    }
    if (hasText(filter.q())) {
      specs.add(nameContains(filter.q()));
    }
    return Specification.allOf(specs);
  }

  public static Specification<Product> categoryEquals(String category) {
    String value = category.strip().toLowerCase(Locale.ROOT);
    return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), value);
  }

  public static Specification<Product> priceAtLeast(BigDecimal min) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
  }

  public static Specification<Product> priceAtMost(BigDecimal max) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
  }

  public static Specification<Product> inStock() {
    return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
  }

  public static Specification<Product> nameContains(String text) {
    String escaped =
        text.strip()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
