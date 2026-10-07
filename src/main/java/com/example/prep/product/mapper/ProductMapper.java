package com.example.prep.product.mapper;

import com.example.prep.product.dto.request.ProductRequest;
import com.example.prep.product.dto.response.ProductResponse;
import com.example.prep.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  public ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getCategory(),
        product.getPrice(),
        product.getStock(),
        product.getRating(),
        product.getCreatedAt());
  }

  public void apply(ProductRequest request, Product product) {
    product.setName(request.name().strip());
    product.setCategory(request.category().strip());
    product.setPrice(request.price());
    product.setStock(request.stock());
    product.setRating(request.rating());
  }
}
