package com.example.prep.product.config;

import com.example.prep.product.entity.Product;
import com.example.prep.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSeeder implements ApplicationRunner {

  static final int SEED_COUNT = 100;

  private static final List<String> CATEGORIES =
      List.of("Electronics", "Books", "Home", "Sports", "Toys", "Beauty");

  private static final Map<String, List<String>> NOUNS =
      Map.of(
          "Electronics",
          List.of("Smartphone", "Headphones", "Earphones", "Laptop", "Monitor", "Phone Charger"),
          "Books",
          List.of("Novel", "Cookbook", "Biography", "Atlas", "Poetry Collection"),
          "Home",
          List.of("Lamp", "Kettle", "Blanket", "Vase", "Cutting Board"),
          "Sports",
          List.of("Yoga Mat", "Football", "Tennis Racket", "Dumbbell", "Water Bottle"),
          "Toys",
          List.of("Puzzle", "Robot", "Doll", "Building Blocks", "Kite"),
          "Beauty",
          List.of("Lipstick", "Face Cream", "Shampoo", "Perfume", "Nail Polish"));

  private static final List<String> ADJECTIVES =
      List.of("Classic", "Compact", "Deluxe", "Eco", "Pro", "Ultra", "Smart", "Vintage");

  private final ProductRepository productRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (productRepository.count() > 0) {
      return;
    }
    Random random = new Random(42);
    List<Product> products = new ArrayList<>(SEED_COUNT);
    for (int i = 0; i < SEED_COUNT; i++) {
      String category = CATEGORIES.get(i % CATEGORIES.size());
      List<String> nouns = NOUNS.get(category);
      Product product = new Product();
      product.setName(
          ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
              + " "
              + nouns.get(random.nextInt(nouns.size())));
      product.setCategory(category);
      product.setPrice(BigDecimal.valueOf(500 + random.nextInt(49_500), 2));
      product.setStock(i % 7 == 0 ? 0 : random.nextInt(200) + 1);
      product.setRating(BigDecimal.valueOf(random.nextInt(51), 1));
      products.add(product);
    }
    productRepository.saveAll(products);
    log.info("Seeded {} products", products.size());
  }
}
