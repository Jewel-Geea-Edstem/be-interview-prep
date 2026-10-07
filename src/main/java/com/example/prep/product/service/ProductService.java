package com.example.prep.product.service;

import com.example.prep.common.exception.BadRequestException;
import com.example.prep.common.exception.NotFoundException;
import com.example.prep.product.config.ProductCache;
import com.example.prep.product.dto.request.ProductFilter;
import com.example.prep.product.dto.request.ProductRequest;
import com.example.prep.product.dto.response.ProductResponse;
import com.example.prep.product.entity.Product;
import com.example.prep.product.mapper.ProductMapper;
import com.example.prep.product.repository.ProductRepository;
import com.example.prep.product.repository.ProductSpecifications;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

  public static final Set<String> SORTABLE =
      Set.of("id", "name", "category", "price", "stock", "rating", "createdAt");

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Product product = new Product();
    productMapper.apply(request, product);
    return productMapper.toResponse(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> list(ProductFilter filter, Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
      if (!SORTABLE.contains(order.getProperty())) {
        throw new BadRequestException(
            "Unsupported sort property '" + order.getProperty() + "', allowed: " + SORTABLE);
      }
    }
    return productRepository
        .findAll(ProductSpecifications.from(filter), pageable)
        .map(productMapper::toResponse);
  }

  @Cacheable(cacheNames = ProductCache.NAME, key = "#id")
  @Transactional(readOnly = true)
  public ProductResponse get(Long id) {
    return productMapper.toResponse(find(id));
  }

  @CachePut(cacheNames = ProductCache.NAME, key = "#id")
  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = find(id);
    productMapper.apply(request, product);
    return productMapper.toResponse(productRepository.saveAndFlush(product));
  }

  @CacheEvict(cacheNames = ProductCache.NAME, key = "#id")
  @Transactional
  public void delete(Long id) {
    productRepository.delete(find(id));
  }

  private Product find(Long id) {
    return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
  }
}
