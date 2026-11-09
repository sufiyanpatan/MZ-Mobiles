package mz.mobile.bussiness.search.api.dto;

import java.util.List;

public record ProductSearchResponse(
        String originalQuery,
        String correctedQuery,
        int totalResults,
        long executionTimeMs,
        List<ProductSearchResult> results
) {
}