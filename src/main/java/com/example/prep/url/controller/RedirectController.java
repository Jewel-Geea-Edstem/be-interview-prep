package com.example.prep.url.controller;

import com.example.prep.url.service.ShortUrlService;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class RedirectController {

  private final ShortUrlService shortUrlService;

  @GetMapping("/{code}")
  public ResponseEntity<Void> redirect(
      @PathVariable
          @Pattern(regexp = "[A-Za-z0-9]{1,8}", message = "code is not a valid short code")
          String code) {
    URI target = URI.create(shortUrlService.resolve(code));
    return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
  }
}
