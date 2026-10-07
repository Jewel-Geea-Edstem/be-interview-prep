package com.example.prep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
  void updateEvictsSoTheNextReadLoadsTheStoredValue() {
    productService.get(id);

    productService.update(id, request("Cache Lamp v2", "12.5"));
    clearInvocations(productRepository);
    ProductResponse afterUpdate = productService.get(id);

    assertThat(afterUpdate.name()).isEqualTo("Cache Lamp v2");
    assertThat(afterUpdate.price()).isEqualTo(new BigDecimal("12.50"));
    verify(productRepository, times(1)).findById(id);
  }

  @Test
  void deleteEvictsTheCachedValue() {
    productService.get(id);

    productService.delete(id);

    assertThat(cacheManager.getCache(ProductCache.NAME).get(id)).isNull();
    assertThrows(NotFoundException.class, () -> productService.get(id));
  }

  @Test
  @Timeout(10)
  void readRacingAnUpdateNeverCachesTheOldValue() throws Exception {
    raceReadAgainst(() -> productService.update(id, request("Cache Lamp v2", "20.00")));

    ProductResponse afterRace = productService.get(id);

    assertThat(afterRace.name()).isEqualTo("Cache Lamp v2");
    assertThat(afterRace.price()).isEqualTo(new BigDecimal("20.00"));
  }

  @Test
  @Timeout(10)
  void readRacingADeleteNeverCachesTheDeletedProduct() throws Exception {
    raceReadAgainst(() -> productService.delete(id));

    assertThat(cacheManager.getCache(ProductCache.NAME).get(id)).isNull();
    assertThrows(NotFoundException.class, () -> productService.get(id));
  }

  private void raceReadAgainst(Runnable writer) throws Exception {
    CountDownLatch readerLoadedOldRow = new CountDownLatch(1);
    CountDownLatch writerCommitted = new CountDownLatch(1);
    AtomicBoolean firstCall = new AtomicBoolean(true);
    Answer<?> realCall =
        mockingDetails(productRepository).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (firstCall.getAndSet(false)) {
                Object oldRow = realCall.answer(invocation);
                readerLoadedOldRow.countDown();
                assertThat(writerCommitted.await(5, TimeUnit.SECONDS)).isTrue();
                return oldRow;
              }
              TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                      writerCommitted.countDown();
                    }
                  });
              return realCall.answer(invocation);
            })
        .when(productRepository)
        .findById(id);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<ProductResponse> reader = executor.submit(() -> productService.get(id));
      assertThat(readerLoadedOldRow.await(5, TimeUnit.SECONDS)).isTrue();

      writer.run();

      assertThat(reader.get(5, TimeUnit.SECONDS).name()).isEqualTo("Cache Lamp");
    } finally {
      executor.shutdownNow();
    }
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
