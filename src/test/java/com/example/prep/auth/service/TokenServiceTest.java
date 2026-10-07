package com.example.prep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.prep.auth.dto.response.TokenResponse;
import com.example.prep.common.security.JwtProperties;
import com.example.prep.user.entity.Role;
import com.example.prep.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

  private final JwtProperties properties =
      new JwtProperties("test-only-secret-not-for-production-0123456789", Duration.ofMinutes(15));
  private final TokenService tokenService =
      new TokenService(
          new NimbusJwtEncoder(new ImmutableSecret<>(properties.signingKey())), properties);

  @Test
  void tokenExpiresFifteenMinutesAfterIssue() {
    TokenResponse response = tokenService.issue(user());

    Jwt jwt = decoderAt(Instant.now()).decode(response.accessToken());

    assertThat(response.expiresIn()).isEqualTo(900);
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(15));
    assertThat(jwt.getSubject()).isEqualTo("user@example.com");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
  }

  @Test
  void tokenIsRejectedAfterFifteenMinutes() {
    TokenResponse response = tokenService.issue(user());
    Instant later = Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(1);

    assertThrows(
        JwtValidationException.class, () -> decoderAt(later).decode(response.accessToken()));
  }

  @Test
  void shortSecretFailsFast() {
    assertThrows(
        IllegalStateException.class, () -> new JwtProperties("test-short", Duration.ofMinutes(15)));
  }

  private NimbusJwtDecoder decoderAt(Instant instant) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(properties.signingKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    JwtTimestampValidator validator = new JwtTimestampValidator(Duration.ZERO);
    validator.setClock(Clock.fixed(instant, ZoneOffset.UTC));
    decoder.setJwtValidator(validator);
    return decoder;
  }

  private User user() {
    User user = new User();
    user.setEmail("user@example.com");
    user.setRole(Role.USER);
    return user;
  }
}
