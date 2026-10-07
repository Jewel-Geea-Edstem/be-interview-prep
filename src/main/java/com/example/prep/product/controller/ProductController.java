package com.example.prep.product.controller;

import com.example.prep.product.dto.request.ProductFilter;
import com.example.prep.product.dto.request.ProductRequest;
import com.example.prep.product.dto.response.ProductResponse;
import com.example.prep.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    ProductResponse created = productService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
  }

  @GetMapping
  public PagedModel<ProductResponse> list(
      @Valid @ModelAttribute ProductFilter filter,
      @PageableDefault(sort = "id") Pageable pageable) {
    return new PagedModel<>(productService.list(filter, pageable));
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable @Positive Long id) {
    return productService.get(id);
  }

  @PutMapping("/{id}")
  public ProductResponse update(
      @PathVariable @Positive Long id, @Valid @RequestBody ProductRequest request) {
    return productService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
