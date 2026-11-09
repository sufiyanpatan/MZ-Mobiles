package mz.mobile.bussiness.search.api.service;

import mz.mobile.bussiness.search.api.dto.ProductSearchResponse;
import mz.mobile.bussiness.search.api.dto.ProductSearchResult;
import mz.mobile.bussiness.search.api.repository.SearchRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductSearchService {

    private final SearchQueryNormalizer queryNormalizer;

    private final SearchRepository searchRepository;

    private static final int MAX_RESULTS = 10;

    public ProductSearchService(
            SearchQueryNormalizer queryNormalizer,
            SearchRepository searchRepository
    ) {

        this.queryNormalizer = queryNormalizer;
        this.searchRepository = searchRepository;
    }

    /**
     * Main search service method.
     */
    public ProductSearchResponse search(
            String originalQuery,
            int requestedLimit
    ) {

        if (requestedLimit < 1) {
            throw new IllegalArgumentException(
                    "Limit must be at least 1"
            );
        }

        // Never return more than 10 products in this milestone.
        int limit = Math.min(requestedLimit, MAX_RESULTS);

        long startTime = System.nanoTime();

        // Step 1: Normalize and correct the query.
        SearchQueryNormalizer.NormalizedQuery normalizedQuery =
                queryNormalizer.normalize(originalQuery);

        // Step 2: Retrieve matching products.
        List<ProductSearchResult> results =
                searchRepository.search(
                        normalizedQuery.correctedTokens(),
                        limit
                );

        // Step 3: Calculate execution time.
        long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startTime
        );

        // Step 4: Prepare the API response.
        return new ProductSearchResponse(
                normalizedQuery.originalQuery(),
                normalizedQuery.correctedQuery(),
                results.size(),
                executionTimeMs,
                results
        );
    }

    /**
     * Checks whether the database is reachable.
     */
    public boolean isDatabaseAvailable() {

        try {
            return searchRepository.pingDatabase();

        } catch (DataAccessException exception) {

            return false;
        }
    }
}