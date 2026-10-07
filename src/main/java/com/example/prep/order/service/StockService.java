package com.example.prep.order.service;

import com.example.prep.common.exception.ConflictException;
import com.example.prep.common.exception.NotFoundException;
import com.example.prep.order.repository.ProductStockRepository;
import com.example.prep.product.config.ProductCache;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

  private final ProductStockRepository productStockRepository;
  private final CacheManager cacheManager;

  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(List<OrderLine> lines) {
    for (OrderLine line : sorted(lines)) {
      if (productStockRepository.reserve(line.productId(), line.quantity()) == 0) {
        throw rejection(line);
      }
      evict(line.productId());
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void release(List<OrderLine> lines) {
    for (OrderLine line : sorted(lines)) {
      productStockRepository.release(line.productId(), line.quantity());
      evict(line.productId());
    }
  }

  private RuntimeException rejection(OrderLine line) {
    return productStockRepository
        .findStock(line.productId())
        .<RuntimeException>map(
            available ->
                new ConflictException(
                    "Insufficient stock for product %d: requested %d, available %d"
                        .formatted(line.productId(), line.quantity(), available)))
        .orElseGet(() -> new NotFoundException("Product", line.productId()));
  }

  private void evict(Long productId) {
    Cache cache = cacheManager.getCache(ProductCache.NAME);
    if (cache != null) {
      cache.evict(productId);
    }
  }

  private static List<OrderLine> sorted(List<OrderLine> lines) {
    return lines.stream().sorted(Comparator.comparing(OrderLine::productId)).toList();
  }
}
