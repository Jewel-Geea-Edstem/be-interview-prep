package com.example.prep.product.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
    @NotBlank(message = "category is required")
        @Size(max = 60, message = "category must be at most 60 characters")
        String category,
    @NotNull(message = "price is required")
        @DecimalMin(value = "0", message = "price must be zero or more")
        @Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimals")
        BigDecimal price,
    @NotNull(message = "stock is required") @PositiveOrZero(message = "stock must be zero or more")
        Integer stock,
    @NotNull(message = "rating is required")
        @DecimalMin(value = "0.0", message = "rating must be between 0.0 and 5.0")
        @DecimalMax(value = "5.0", message = "rating must be between 0.0 and 5.0")
        @Digits(integer = 1, fraction = 1, message = "rating must have at most 1 decimal")
        BigDecimal rating) {}
