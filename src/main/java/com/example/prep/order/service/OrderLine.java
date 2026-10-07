package com.example.prep.order.service;

import com.example.prep.order.dto.request.OrderItemRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record OrderLine(Long productId, int quantity) {

  public static List<OrderLine> canonical(List<OrderItemRequest> items) {
    Map<Long, Integer> merged =
        items.stream()
            .collect(
                Collectors.toMap(
                    OrderItemRequest::productId,
                    OrderItemRequest::quantity,
                    Integer::sum,
                    TreeMap::new));
    return merged.entrySet().stream()
        .map(entry -> new OrderLine(entry.getKey(), entry.getValue()))
        .toList();
  }

  public static String hash(List<OrderLine> lines) {
    String canonical =
        lines.stream()
            .map(line -> line.productId() + ":" + line.quantity())
            .collect(Collectors.joining(","));
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
