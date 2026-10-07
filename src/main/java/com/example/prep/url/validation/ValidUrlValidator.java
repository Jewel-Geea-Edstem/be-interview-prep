package com.example.prep.url.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class ValidUrlValidator implements ConstraintValidator<ValidUrl, String> {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    try {
      URI uri = new URI(value.strip());
      return uri.isAbsolute()
          && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase())
          && uri.getHost() != null
          && !uri.getHost().isBlank();
    } catch (URISyntaxException ex) {
      return false;
    }
  }
}
