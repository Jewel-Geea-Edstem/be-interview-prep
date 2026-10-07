package com.example.prep.url.mapper;

import com.example.prep.url.config.ShortUrlProperties;
import com.example.prep.url.dto.response.ShortUrlResponse;
import com.example.prep.url.dto.response.ShortUrlStatsResponse;
import com.example.prep.url.entity.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortUrlMapper {

  private final ShortUrlProperties properties;

  public ShortUrlResponse toResponse(ShortUrl shortUrl) {
    return new ShortUrlResponse(
        shortUrl.getCode(),
        shortUrlFor(shortUrl.getCode()),
        shortUrl.getOriginalUrl(),
        shortUrl.getExpiresAt(),
        shortUrl.getCreatedAt());
  }

  public ShortUrlStatsResponse toStats(ShortUrl shortUrl) {
    return new ShortUrlStatsResponse(
        shortUrl.getCode(),
        shortUrlFor(shortUrl.getCode()),
        shortUrl.getOriginalUrl(),
        shortUrl.getVisitCount(),
        shortUrl.getCreatedAt(),
        shortUrl.getExpiresAt());
  }

  private String shortUrlFor(String code) {
    String base = properties.baseUrl();
    String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    return trimmed + "/r/" + code;
  }
}
