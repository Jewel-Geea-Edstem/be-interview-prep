package com.example.prep.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration ttl) {

  private static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "app.jwt.secret (JWT_SECRET) must be set and at least "
              + MIN_SECRET_BYTES
              + " bytes long");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalStateException("app.jwt.ttl (JWT_TTL) must be a positive duration");
    }
  }

  public SecretKey signingKey() {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
