package mz.mobile.bussiness.search.api.dto;

import java.math.BigDecimal;

public record ProductSearchResult(
        Long id,
        String brand,
        String productName,
        String model,
        BigDecimal price,
        double relevanceScore
) {
}