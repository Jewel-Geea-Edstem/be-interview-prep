package com.example.prep.url.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "short_urls",
    uniqueConstraints = @UniqueConstraint(name = "uk_short_urls_code", columnNames = "code"),
    indexes = @Index(name = "idx_short_urls_original_url", columnList = "original_url"))
@Getter
@Setter
@NoArgsConstructor
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 8)
  private String code;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(nullable = false)
  private long visitCount;

  private Instant expiresAt;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  public boolean isExpired(Instant now) {
    return expiresAt != null && !expiresAt.isAfter(now);
  }
}
