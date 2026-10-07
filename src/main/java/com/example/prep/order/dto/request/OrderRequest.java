package com.example.prep.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
    @NotEmpty(message = "items must not be empty")
        @Size(max = 50, message = "items must contain at most 50 entries")
        List<@Valid @NotNull(message = "item must not be null") OrderItemRequest> items) {}
