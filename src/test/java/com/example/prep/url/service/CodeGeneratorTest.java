package com.example.prep.url.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodeGeneratorTest {

  private final CodeGenerator codeGenerator = new CodeGenerator();

  @Test
  void codesAreShortAndUrlSafe() {
    for (int i = 0; i < 1000; i++) {
      String code = codeGenerator.generate();
      assertThat(code).hasSizeLessThanOrEqualTo(8).matches("[A-Za-z0-9]+");
    }
  }

  @Test
  void codesAreRandom() {
    Set<String> codes = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      codes.add(codeGenerator.generate());
    }
    assertThat(codes).hasSize(1000);
  }
}
