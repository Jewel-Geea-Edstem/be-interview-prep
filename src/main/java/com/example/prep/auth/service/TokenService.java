package com.example.prep.auth.service;

import com.example.prep.auth.dto.response.TokenResponse;
import com.example.prep.common.security.JwtProperties;
import com.example.prep.common.security.SecurityConfig;
import com.example.prep.user.entity.User;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

  private static final String TOKEN_TYPE = "Bearer";

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  public TokenResponse issue(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getEmail())
            .issuedAt(now)
            .expiresAt(now.plus(jwtProperties.ttl()))
            .claim(SecurityConfig.ROLES_CLAIM, List.of(user.getRole().name()))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenResponse(token, TOKEN_TYPE, jwtProperties.ttl().toSeconds());
  }
}
