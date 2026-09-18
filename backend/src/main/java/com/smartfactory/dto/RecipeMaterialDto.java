package com.smartfactory.dto;

import java.math.BigDecimal;

public record RecipeMaterialDto(
    Long id,
    Long materialId,
    String materialName,
    String materialCode,
    String materialType,
    String materialCategory,
    BigDecimal quantityPerBatch,
    String unit,
    Boolean isOptional,
    Integer bomOrder
) {}
