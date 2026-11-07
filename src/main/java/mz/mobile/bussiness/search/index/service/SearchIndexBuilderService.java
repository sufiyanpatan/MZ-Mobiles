
package mz.mobile.bussiness.search.index.service;

import mz.mobile.bussiness.search.index.dto.SearchIndexBuildResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SearchIndexBuilderService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean indexing = new AtomicBoolean(false);

    private static final int TERM_QUERY_CHUNK_SIZE = 300;

    private static final String PRODUCT_SELECT_SQL = """
            SELECT id, category, brand, product_name,
                   model, description
            FROM products
            WHERE id > ?
            ORDER BY id
            LIMIT ?
            """;

    private static final String INDEX_INSERT_SQL = """
            INSERT INTO product_search_index
            (
                product_id,
                normalized_name,
                normalized_brand,
                normalized_model,
                searchable_text
            )
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                normalized_name = VALUES(normalized_name),
                normalized_brand = VALUES(normalized_brand),
                normalized_model = VALUES(normalized_model),
                searchable_text = VALUES(searchable_text)
            """;

    private static final String VOCABULARY_INSERT_SQL = """
            INSERT INTO search_vocabulary (term)
            VALUES (?)
            ON DUPLICATE KEY UPDATE term = VALUES(term)
            """;

    private static final String PRODUCT_TERMS_INSERT_SQL = """
            INSERT INTO product_search_terms
            (
                term_id,
                product_id,
                field_weight
            )
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                field_weight = GREATEST(
                    field_weight,
                    VALUES(field_weight)
                )
            """;

    private static final Map<String, String> ALIASES = Map.of(
            "iphne", "iphone",
            "iphon", "iphone",
            "iphnoe", "iphone",
            "samung", "samsung",
            "samsng", "samsung",
            "macbok", "macbook",
            "appl", "apple"
    );

    public SearchIndexBuilderService(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {

        this.jdbcTemplate = jdbcTemplate;

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    public SearchIndexBuildResponse rebuildIndex(long maxProducts, int batchSize) {

        if (maxProducts < 1) {
            throw new IllegalArgumentException(
                    "MaxProducts must be greater than zero");
        }

        if (batchSize < 100 || batchSize > 2000) {
            throw new IllegalArgumentException(
                    "Batch size must be between 100 and 2000");
        }

        if (!indexing.compareAndSet(false, true)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Search indexing is already running");
        }

        long startTime = System.currentTimeMillis();

        try {

            // Step 1: Clear previous search data
            clearExistingIndex();

            long totalProducts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products",
                    Long.class
            );

            long targetProducts =
                    Math.min(totalProducts, maxProducts);

            long lastProcessedId = 0;
            long indexedProducts = 0;
            long totalMappings = 0;

            // Step 2: Read products in batches
            while (indexedProducts < targetProducts) {

                int currentBatchSize = (int) Math.min(
                        batchSize,
                        targetProducts - indexedProducts
                );

                List<ProductRow> products = jdbcTemplate.query(
                        PRODUCT_SELECT_SQL,
                        (rs, rowNum) -> new ProductRow(
                                rs.getLong("id"),
                                rs.getString("category"),
                                rs.getString("brand"),
                                rs.getString("product_name"),
                                rs.getString("model"),
                                rs.getString("description")
                        ),
                        lastProcessedId,
                        currentBatchSize
                );

                if (products.isEmpty()) {
                    break;
                }

                BatchResult result = transactionTemplate.execute(
                        status -> processBatch(products)
                );

                if (result == null) {
                    throw new IllegalStateException(
                            "Indexing batch returned no result");
                }

                indexedProducts += products.size();
                totalMappings += result.mappingCount();

                lastProcessedId =
                        products.get(products.size() - 1).id();
            }

            // Step 3: Calculate how many products contain each term
            updateDocumentFrequency();

            // Step 4: Populate predefined spelling corrections
            long aliasesInserted = populateAliases();

            // Step 5: Collect final counts
            long vocabularyCount = countRows(
                    "search_vocabulary"
            );

            long executionTime =
                    System.currentTimeMillis() - startTime;

            return new SearchIndexBuildResponse(
                    totalProducts,
                    indexedProducts,
                    vocabularyCount,
                    totalMappings,
                    aliasesInserted,
                    batchSize,
                    executionTime,
                    "Search index rebuilt successfully"
            );

        } finally {
            indexing.set(false);
        }
    }

    private void clearExistingIndex() {

        transactionTemplate.executeWithoutResult(status -> {

            jdbcTemplate.update("DELETE FROM search_alias");

            jdbcTemplate.update(
                    "DELETE FROM product_search_terms"
            );

            jdbcTemplate.update(
                    "DELETE FROM product_search_index"
            );

            jdbcTemplate.update(
                    "DELETE FROM search_vocabulary"
            );
        });
    }

    private BatchResult processBatch(List<ProductRow> products) {

        List<SearchDocument> documents = new ArrayList<>();

        List<Object[]> indexBatch = new ArrayList<>();

        Set<String> uniqueTerms = new LinkedHashSet<>();

        // Step 1: Normalize each product
        for (ProductRow product : products) {

            String normalizedName =
                    normalize(product.productName());

            String normalizedBrand =
                    normalize(product.brand());

            String normalizedModel =
                    normalize(product.model());

            String normalizedCategory =
                    normalize(product.category());

            String normalizedDescription =
                    normalize(product.description());

            String searchableText = String.join(
                    " ",
                    Arrays.asList(
                                    normalizedCategory,
                                    normalizedBrand,
                                    normalizedName,
                                    normalizedModel,
                                    normalizedDescription
                            ).stream()
                            .filter(s -> !s.isBlank())
                            .toList()
            );

            Map<String, Integer> termWeights =
                    new LinkedHashMap<>();

            addTerms(termWeights, normalizedName, 5);
            addTerms(termWeights, normalizedBrand, 4);
            addTerms(termWeights, normalizedModel, 4);
            addTerms(termWeights, normalizedCategory, 2);
            addTerms(termWeights, normalizedDescription, 1);

            uniqueTerms.addAll(termWeights.keySet());

            documents.add(new SearchDocument(
                    product.id(),
                    termWeights
            ));

            indexBatch.add(new Object[]{
                    product.id(),
                    normalizedName,
                    normalizedBrand,
                    normalizedModel.isBlank()
                            ? null : normalizedModel,
                    searchableText
            });
        }

        // Step 2: Insert normalized product information
        jdbcTemplate.batchUpdate(
                INDEX_INSERT_SQL,
                indexBatch
        );

        // Step 3: Insert unique words into vocabulary
        List<Object[]> vocabularyBatch = uniqueTerms
                .stream()
                .map(term -> new Object[]{term})
                .toList();

        if (!vocabularyBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    VOCABULARY_INSERT_SQL,
                    vocabularyBatch
            );
        }

        // Step 4: Retrieve vocabulary IDs
        Map<String, Long> termIds =
                findVocabularyIds(uniqueTerms);

        // Step 5: Map words to their products
        List<Object[]> mappingBatch = new ArrayList<>();

        for (SearchDocument document : documents) {

            for (Map.Entry<String, Integer> entry
                    : document.termWeights().entrySet()) {

                Long termId = termIds.get(entry.getKey());

                if (termId == null) {
                    throw new IllegalStateException(
                            "Vocabulary term missing: "
                                    + entry.getKey()
                    );
                }

                mappingBatch.add(new Object[]{
                        termId,
                        document.productId(),
                        entry.getValue()
                });
            }
        }

        if (!mappingBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    PRODUCT_TERMS_INSERT_SQL,
                    mappingBatch
            );
        }

        return new BatchResult(mappingBatch.size());
    }

    private Map<String, Long> findVocabularyIds(Set<String> terms) {

        Map<String, Long> result = new HashMap<>();

        List<String> allTerms = new ArrayList<>(terms);

        for (int start = 0; start < allTerms.size(); start += TERM_QUERY_CHUNK_SIZE) {

            int end = Math.min(
                    start + TERM_QUERY_CHUNK_SIZE,
                    allTerms.size()
            );

            List<String> chunk =
                    allTerms.subList(start, end);

            String placeholders = String.join(
                    ",",
                    Collections.nCopies(chunk.size(), "?")
            );

            String sql = """
                    SELECT term_id, term
                    FROM search_vocabulary
                    WHERE term IN (
                    """ + placeholders + ")";

//            jdbcTemplate.query(
//                    sql,
//                    (ResultSetExtractor<Long>) rs -> result.put(
//                            rs.getString("term"),
//                            rs.getLong("term_id")
//                    ),
//                    chunk.toArray()
//            );
            jdbcTemplate.query(
                    sql,
                    (ResultSetExtractor<Void>) rs -> {
                        while (rs.next()) {
                            result.put(
                                    rs.getString("term"),
                                    rs.getLong("term_id")
                            );
                        }
                        return null;
                    },
                    chunk.toArray()
            );
        }

        return result;
    }

    private void addTerms(
            Map<String, Integer> termWeights,
            String text,
            int weight) {

        if (text == null || text.isBlank()) {
            return;
        }

        for (String term : text.split("\\s+")) {
            if (term.isBlank()) {
                continue;
            }
            termWeights.merge(
                    term,
                    weight,
                    Math::max
            );
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private void updateDocumentFrequency() {

        jdbcTemplate.update("""
                UPDATE search_vocabulary v
                JOIN (
                    SELECT term_id, COUNT(*) AS total
                    FROM product_search_terms
                    GROUP BY term_id
                ) t ON t.term_id = v.term_id
                SET v.document_frequency = t.total
                """);
    }

    private long populateAliases() {

        String sql = """
                INSERT INTO search_alias
                (
                    alias_term,
                    canonical_term_id
                )
                SELECT ?, term_id
                FROM search_vocabulary
                WHERE term = ?
                ON DUPLICATE KEY UPDATE
                    canonical_term_id =
                        VALUES(canonical_term_id)
                """;

        long inserted = 0;

        for (Map.Entry<String, String> alias
                : ALIASES.entrySet()) {

            inserted += jdbcTemplate.update(
                    sql,
                    alias.getKey(),
                    alias.getValue()
            );
        }

        return inserted;
    }

    private long countRows(String tableName) {

        // Only internal, fixed table names are passed here.
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
    }

    private record ProductRow(
            long id,
            String category,
            String brand,
            String productName,
            String model,
            String description
    ) {
    }

    private record SearchDocument(
            long productId,
            Map<String, Integer> termWeights
    ) {
    }

    private record BatchResult(
            long mappingCount
    ) {
    }
}
