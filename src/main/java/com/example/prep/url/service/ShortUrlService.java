package com.example.prep.url.service;

import com.example.prep.common.exception.GoneException;
import com.example.prep.common.exception.NotFoundException;
import com.example.prep.url.dto.request.ShortenRequest;
import com.example.prep.url.dto.response.ShortUrlStatsResponse;
import com.example.prep.url.entity.ShortUrl;
import com.example.prep.url.mapper.ShortUrlMapper;
import com.example.prep.url.repository.ShortUrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

  private static final int MAX_CODE_ATTEMPTS = 5;
  private static final String RESOURCE = "Short URL";

  private final ShortUrlRepository shortUrlRepository;
  private final ShortUrlMapper shortUrlMapper;
  private final CodeGenerator codeGenerator;

  @Transactional
  public ShortenResult shorten(ShortenRequest request) {
    String originalUrl = request.url().strip();
    Instant expiresAt =
        request.expiresAt() == null ? null : request.expiresAt().truncatedTo(ChronoUnit.MILLIS);
    Optional<ShortUrl> existing = findActive(originalUrl, expiresAt);
    if (existing.isPresent()) {
      return new ShortenResult(shortUrlMapper.toResponse(existing.get()), false);
    }
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setCode(newCode());
    shortUrl.setOriginalUrl(originalUrl);
    shortUrl.setExpiresAt(expiresAt);
    return new ShortenResult(
        shortUrlMapper.toResponse(shortUrlRepository.saveAndFlush(shortUrl)), true);
  }

  @Transactional
  public String resolve(String code) {
    ShortUrl shortUrl = find(code);
    if (shortUrl.isExpired(Instant.now())) {
      throw new GoneException(RESOURCE, code);
    }
    shortUrlRepository.incrementVisitCount(shortUrl.getId());
    return shortUrl.getOriginalUrl();
  }

  @Transactional(readOnly = true)
  public ShortUrlStatsResponse stats(String code) {
    return shortUrlMapper.toStats(find(code));
  }

  private Optional<ShortUrl> findActive(String originalUrl, Instant expiresAt) {
    Instant now = Instant.now();
    return shortUrlRepository.findByOriginalUrlAndExpiresAt(originalUrl, expiresAt).stream()
        .filter(shortUrl -> !shortUrl.isExpired(now))
        .findFirst();
  }

  private String newCode() {
    for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
      String code = codeGenerator.generate();
      if (!shortUrlRepository.existsByCode(code)) {
        return code;
      }
    }
    throw new IllegalStateException("Could not generate a unique short code");
  }

  private ShortUrl find(String code) {
    return shortUrlRepository
        .findByCode(code)
        .orElseThrow(() -> new NotFoundException(RESOURCE, code));
  }
}
