package com.example.prep.url.repository;

import com.example.prep.url.entity.ShortUrl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

  Optional<ShortUrl> findByCode(String code);

  boolean existsByCode(String code);

  List<ShortUrl> findByOriginalUrlAndExpiresAt(String originalUrl, Instant expiresAt);

  @Modifying
  @Query("update ShortUrl s set s.visitCount = s.visitCount + 1 where s.id = :id")
  int incrementVisitCount(@Param("id") Long id);
}
