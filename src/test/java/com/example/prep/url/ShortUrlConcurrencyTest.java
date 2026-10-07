package com.example.prep.url;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prep.url.entity.ShortUrl;
import com.example.prep.url.repository.ShortUrlRepository;
import com.example.prep.url.service.ShortUrlService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortUrlConcurrencyTest {

  private static final int VISITS = 50;

  @Autowired private ShortUrlService shortUrlService;
  @Autowired private ShortUrlRepository shortUrlRepository;

  @Test
  void concurrentVisitsAreAllCounted() throws Exception {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setCode("conc0001");
    shortUrl.setOriginalUrl("https://example.com/concurrent");
    ShortUrl saved = shortUrlRepository.save(shortUrl);

    ExecutorService executor = Executors.newFixedThreadPool(16);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<String>> results = new ArrayList<>();
      for (int i = 0; i < VISITS; i++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return shortUrlService.resolve(saved.getCode());
                }));
      }
      start.countDown();
      for (Future<String> result : results) {
        assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo("https://example.com/concurrent");
      }
    } finally {
      executor.shutdownNow();
    }

    assertThat(shortUrlRepository.findById(saved.getId()))
        .hasValueSatisfying(reloaded -> assertThat(reloaded.getVisitCount()).isEqualTo(VISITS));
  }
}
