
package mz.mobile.bussiness.search.api.repository;

import mz.mobile.bussiness.search.api.dto.ProductSearchResult;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SearchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_TERM_EXPANSIONS = 30;

    public SearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductSearchResult> search(
            List<String> tokens,
            int limit
    ) {

        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        List<QueryTermMatch> queryMatches = new ArrayList<>();

        // Resolve every search word to vocabulary IDs.
        for (int tokenIndex = 0;
             tokenIndex < tokens.size();
             tokenIndex++) {

            String token = tokens.get(tokenIndex);

            boolean allowPrefixSearch =
                    token.length() >= 2
                            || isSingleDigit(token)
                            || (token.length() == 1 && tokenIndex > 0);

            List<VocabularyTerm> vocabularyTerms =
                    findVocabularyTerms(token, allowPrefixSearch);

            /*
             * Require every query word to match.
             *
             * Example:
             * "iphone xyz"
             *
             * Should not return every iPhone.
             */
            if (vocabularyTerms.isEmpty()) {
                return List.of();
            }

            for (VocabularyTerm vocabularyTerm : vocabularyTerms) {

                queryMatches.add(
                        new QueryTermMatch(
                                tokenIndex,
                                vocabularyTerm.termId(),
                                vocabularyTerm.term().equals(token)
                        )
                );
            }
        }

        // Fast path for a single exact vocabulary term.
        if (tokens.size() == 1 && queryMatches.size() == 1) {

            return executeSingleTermSearch(
                    queryMatches.get(0).termId(),
                    limit
            );
        }

        // Multiple words or expanded prefixes.
        return executeProductSearch(
                queryMatches,
                tokens.size(),
                limit
        );
    }

    private List<VocabularyTerm> findVocabularyTerms(
            String token,
            boolean allowPrefixSearch
    ) {

        if (!allowPrefixSearch) {

            return jdbcTemplate.query(
                    """
                    SELECT term_id, term
                    FROM search_vocabulary
                    WHERE term = ?
                    LIMIT 1
                    """,

                    (rs, rowNum) -> new VocabularyTerm(
                            rs.getLong("term_id"),
                            rs.getString("term")
                    ),

                    token
            );
        }

        /*
         * Numeric-prefix special handling.
         *
         * "iphone 1" should match 11, 12, 13.
         *
         * It should not expand the numeric token into
         * storage terms such as 128gb.
         */
        String sql = isSingleDigit(token)

                ? """
                  SELECT term_id, term
                  FROM search_vocabulary
                  WHERE term LIKE ?
                    AND term REGEXP '^[0-9]+$'
                  ORDER BY
                      CASE WHEN term = ? THEN 0 ELSE 1 END,
                      CHAR_LENGTH(term) ASC,
                      document_frequency DESC
                  LIMIT ?
                  """

                : """
                  SELECT term_id, term
                  FROM search_vocabulary
                  WHERE term LIKE ?
                  ORDER BY
                      CASE WHEN term = ? THEN 0 ELSE 1 END,
                      CHAR_LENGTH(term) ASC,
                      document_frequency DESC
                  LIMIT ?
                  """;

        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new VocabularyTerm(
                        rs.getLong("term_id"),
                        rs.getString("term")
                ),

                token + "%",
                token,
                MAX_TERM_EXPANSIONS
        );
    }

    /*
     * Optimized single-word search.
     */
    private List<ProductSearchResult> executeSingleTermSearch(
            long termId,
            int limit
    ) {

        String sql = """
                SELECT
                    p.id,
                    p.brand,
                    p.product_name,
                    p.model,
                    p.price,
                    CAST(
                        pst.field_weight AS DECIMAL(10,2)
                    ) AS relevance_score

                FROM product_search_terms pst

                JOIN products p
                    ON p.id = pst.product_id

                WHERE pst.term_id = ?

                ORDER BY
                    pst.field_weight DESC,
                    pst.product_id DESC

                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new ProductSearchResult(
                        rs.getLong("id"),
                        rs.getString("brand"),
                        rs.getString("product_name"),
                        rs.getString("model"),
                        rs.getBigDecimal("price"),
                        rs.getDouble("relevance_score")
                ),

                termId,
                limit
        );
    }

    /*
     * Multi-word search.
     *
     * A product must match every query token.
     */
    private List<ProductSearchResult> executeProductSearch(
            List<QueryTermMatch> queryMatches,
            int expectedTokenCount,
            int limit
    ) {

        StringBuilder sql = new StringBuilder();

        List<Object> parameters = new ArrayList<>();

        sql.append("WITH query_matches AS (");

        for (int i = 0; i < queryMatches.size(); i++) {

            if (i > 0) {
                sql.append(" UNION ALL ");
            }

            sql.append("""
                    SELECT
                        ? AS token_no,
                        ? AS term_id,
                        ? AS exact_match
                    """);

            QueryTermMatch match = queryMatches.get(i);

            parameters.add(match.tokenNo());

            parameters.add(match.termId());

            parameters.add(match.exactMatch() ? 1 : 0);
        }

        sql.append("""
                ),

                per_product_token AS (

                    SELECT
                        pst.product_id,
                        qm.token_no,

                        MAX(
                            CASE
                                WHEN qm.exact_match = 1
                                THEN pst.field_weight
                                ELSE 0
                            END
                        ) AS exact_weight,

                        MAX(pst.field_weight) AS any_weight

                    FROM query_matches qm

                    JOIN product_search_terms pst
                        ON pst.term_id = qm.term_id

                    GROUP BY
                        pst.product_id,
                        qm.token_no
                ),

                scored_products AS (

                    SELECT
                        product_id,

                        COUNT(DISTINCT token_no)
                            AS matched_token_count,

                        SUM(
                            CASE
                                WHEN exact_weight > 0
                                THEN exact_weight
                                ELSE any_weight * 0.65
                            END
                        ) AS raw_score,

                        SUM(
                            CASE
                                WHEN exact_weight > 0
                                THEN 1
                                ELSE 0
                            END
                        ) AS exact_token_count

                    FROM per_product_token

                    GROUP BY product_id

                    HAVING COUNT(DISTINCT token_no) = ?
                )

                SELECT
                    p.id,
                    p.brand,
                    p.product_name,
                    p.model,
                    p.price,

                    ROUND(
                        s.raw_score
                        + (s.matched_token_count * 5.0)
                        + (s.exact_token_count * 2.0),
                        2
                    ) AS relevance_score

                FROM scored_products s

                JOIN products p
                    ON p.id = s.product_id

                ORDER BY
                    s.matched_token_count DESC,
                    s.exact_token_count DESC,
                    relevance_score DESC,
                    p.id DESC

                LIMIT ?
                """);

        parameters.add(expectedTokenCount);

        parameters.add(limit);

        return jdbcTemplate.query(
                sql.toString(),

                (rs, rowNum) -> new ProductSearchResult(
                        rs.getLong("id"),
                        rs.getString("brand"),
                        rs.getString("product_name"),
                        rs.getString("model"),
                        rs.getBigDecimal("price"),
                        rs.getDouble("relevance_score")
                ),

                parameters.toArray()
        );
    }

    private boolean isSingleDigit(String token) {

        return token.length() == 1
                && Character.isDigit(token.charAt(0));
    }

    public boolean pingDatabase() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        return result != null && result == 1;
    }

    private record VocabularyTerm(
            long termId,
            String term
    ) {
    }

    private record QueryTermMatch(
            int tokenNo,
            long termId,
            boolean exactMatch
    ) {
    }
}
