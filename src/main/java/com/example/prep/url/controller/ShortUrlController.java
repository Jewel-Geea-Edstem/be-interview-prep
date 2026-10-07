package com.example.prep.url.controller;

import com.example.prep.url.dto.request.ShortenRequest;
import com.example.prep.url.dto.response.ShortUrlResponse;
import com.example.prep.url.dto.response.ShortUrlStatsResponse;
import com.example.prep.url.service.ShortUrlService;
import com.example.prep.url.service.ShortenResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class ShortUrlController {

  private final ShortUrlService shortUrlService;

  @PostMapping
  public ResponseEntity<ShortUrlResponse> shorten(@Valid @RequestBody ShortenRequest request) {
    ShortenResult result = shortUrlService.shorten(request);
    ShortUrlResponse body = result.shortUrl();
    if (!result.created()) {
      return ResponseEntity.ok(body);
    }
    return ResponseEntity.created(URI.create("/api/v1/urls/" + body.code() + "/stats")).body(body);
  }

  @GetMapping("/{code}/stats")
  public ShortUrlStatsResponse stats(
      @PathVariable
          @Pattern(regexp = "[A-Za-z0-9]{1,8}", message = "code is not a valid short code")
          String code) {
    return shortUrlService.stats(code);
  }
}
