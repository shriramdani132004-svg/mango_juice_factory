package com.smartfactory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateOrderRequest(
    @NotNull(message = "Product ID is required")
    Long productId,

    @NotNull(message = "Mango quantity is required")
    @DecimalMin(value = "0.01", message = "Mango quantity must be greater than 0")
    BigDecimal mangoQuantityKg,

    @NotNull(message = "Bottle size is required")
    @Min(value = 1, message = "Bottle size must be at least 1 mL")
    Integer bottleSizeMl,

    @NotNull(message = "Priority is required")
    Integer priority,

    LocalDateTime deadline
) {}
