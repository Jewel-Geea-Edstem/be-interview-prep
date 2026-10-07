package com.example.prep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.prep.common.exception.NotFoundException;
import com.example.prep.product.config.ProductCache;
import com.example.prep.product.dto.request.ProductRequest;
import com.example.prep.product.dto.response.ProductResponse;
import com.example.prep.product.repository.ProductRepository;
import com.example.prep.product.service.ProductService;
import com.github.benmanes.caffeine.cache.Cache;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:product-cache;DB_CLOSE_DELAY=-1")
class ProductCacheTest {

  @Autowired private ProductService productService;
  @Autowired private CacheManager cacheManager;
  @MockitoSpyBean private ProductRepository productRepository;

  private Long id;

  @BeforeEach
  void setUp() {
    cacheManager.getCache(ProductCache.NAME).clear();
    id = productService.create(request("Cache Lamp", "10.00")).id();
    clearInvocations(productRepository);
  }

  @Test
  void repeatedLookupsHitTheDatabaseOnce() {
    long hitsBefore = nativeCache().stats().hitCount();

    ProductResponse first = productService.get(id);
    ProductResponse second = productService.get(id);

    assertThat(second).isEqualTo(first);
    verify(productRepository, times(1)).findById(id);
    assertThat(nativeCache().stats().hitCount()).isEqualTo(hitsBefore + 1);
  }

  @Test
  void updateRefreshesTheCachedValue() {
    productService.get(id);

    productService.update(id, request("Cache Lamp v2", "12.50"));
    clearInvocations(productRepository);
    ProductResponse afterUpdate = productService.get(id);

    assertThat(afterUpdate.name()).isEqualTo("Cache Lamp v2");
    assertThat(afterUpdate.price()).isEqualByComparingTo("12.50");
    verify(productRepository, never()).findById(anyLong());
  }

  @Test
  void deleteEvictsTheCachedValue() {
    productService.get(id);

    productService.delete(id);

    assertThat(cacheManager.getCache(ProductCache.NAME).get(id)).isNull();
    assertThrows(NotFoundException.class, () -> productService.get(id));
  }

  private Cache<Object, Object> nativeCache() {
    TransactionAwareCacheDecorator decorator =
        (TransactionAwareCacheDecorator) cacheManager.getCache(ProductCache.NAME);
    return ((CaffeineCache) decorator.getTargetCache()).getNativeCache();
  }

  private ProductRequest request(String name, String price) {
    return new ProductRequest(name, "Home", new BigDecimal(price), 5, new BigDecimal("4.0"));
  }
}
