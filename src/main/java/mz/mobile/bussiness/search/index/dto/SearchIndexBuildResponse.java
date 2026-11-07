
package mz.mobile.bussiness.search.index.dto;

public record SearchIndexBuildResponse(
        long totalProductsAvailable,
        long productsIndexed,
        long vocabularyTerms,
        long productTermMappings,
        long aliasesInserted,
        int batchSize,
        long executionTimeMs,
        String message
) {
}
