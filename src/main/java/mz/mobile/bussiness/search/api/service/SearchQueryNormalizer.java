
package mz.mobile.bussiness.search.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SearchQueryNormalizer {

    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_QUERY_LENGTH = 100;

    private static final int MAX_QUERY_TOKENS = 8;

    private static final int MAX_FUZZY_CANDIDATES = 100;

    public SearchQueryNormalizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public NormalizedQuery normalize(String originalQuery) {

        if (originalQuery == null || originalQuery.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query cannot be empty"
            );
        }

        if (originalQuery.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Search query cannot exceed 100 characters"
            );
        }

        String cleanedQuery = cleanQuery(originalQuery);

        if (cleanedQuery.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must contain letters or numbers"
            );
        }

        List<String> originalTokens =
                Arrays.asList(cleanedQuery.split("\\s+"));

        if (originalTokens.size() > MAX_QUERY_TOKENS) {
            throw new IllegalArgumentException(
                    "Search query cannot contain more than 8 words"
            );
        }

        // First check manually configured aliases.
        Map<String, String> aliases = findAliases(
                originalTokens.stream().distinct().toList()
        );

        Set<String> correctedTokens = new LinkedHashSet<>();

        for (String token : originalTokens) {

            String correctedToken = aliases.get(token);

            if (correctedToken == null) {
                correctedToken = correctUsingVocabulary(token);
            }

            correctedTokens.add(correctedToken);
        }

        List<String> finalTokens = List.copyOf(correctedTokens);

        String correctedQuery = String.join(" ", finalTokens);

        return new NormalizedQuery(
                originalQuery,
                cleanedQuery,
                correctedQuery,
                originalTokens,
                finalTokens
        );
    }

    private String cleanQuery(String query) {

        return query.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private Map<String, String> findAliases(List<String> tokens) {

        if (tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = tokens.stream()
                .map(token -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT
                    a.alias_term,
                    v.term AS canonical_term
                FROM search_alias a
                JOIN search_vocabulary v
                    ON a.canonical_term_id = v.term_id
                WHERE a.alias_term IN (
                """ + placeholders + ")";

        Map<String, String> aliases = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {

            while (rs.next()) {

                aliases.put(
                        rs.getString("alias_term"),
                        rs.getString("canonical_term")
                );
            }

            return null;

        }, tokens.toArray());

        return aliases;
    }

    private String correctUsingVocabulary(String token) {

        // Don't perform fuzzy correction for short words or numbers.
        if (token.length() < 4 || isNumeric(token)) {
            return token;
        }

        if (vocabularyTermExists(token)) {
            return token;
        }

        return findFuzzyCorrection(token).orElse(token);
    }

    /*
     * IMPORTANT FIX:
     *
     * A missing vocabulary term must return false.
     * It must not throw an exception.
     */

    private boolean vocabularyTermExists(String token) {

        return Boolean.TRUE.equals(
                jdbcTemplate.query(
                        """
                        SELECT 1
                        FROM search_vocabulary
                        WHERE term = ?
                        LIMIT 1
                        """,
                        (org.springframework.jdbc.core.ResultSetExtractor<Boolean>) rs -> rs.next(),
                        token
                )
        );
    }


    private Optional<String> findFuzzyCorrection(String token) {

        String sql = """
                SELECT term
                FROM search_vocabulary
                WHERE first_char = ?
                  AND term_length BETWEEN ? AND ?
                ORDER BY document_frequency DESC
                LIMIT ?
                """;

        int minimumLength = Math.max(1, token.length() - 1);

        int maximumLength = token.length() + 1;

        List<String> candidates = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("term"),
                token.substring(0, 1),
                minimumLength,
                maximumLength,
                MAX_FUZZY_CANDIDATES
        );

        return candidates.stream()
                .filter(candidate ->
                        isAtMostOneEditAway(token, candidate)
                )
                .findFirst();
    }

    /*
     * Supports:
     * One insertion
     * One deletion
     * One substitution
     * Adjacent-character transposition
     */
    private boolean isAtMostOneEditAway(
            String first,
            String second
    ) {

        int firstLength = first.length();

        int secondLength = second.length();

        if (Math.abs(firstLength - secondLength) > 1) {
            return false;
        }

        if (firstLength == secondLength) {

            int firstMismatch = -1;

            int secondMismatch = -1;

            for (int i = 0; i < firstLength; i++) {

                if (first.charAt(i) != second.charAt(i)) {

                    if (firstMismatch == -1) {
                        firstMismatch = i;
                    } else if (secondMismatch == -1) {
                        secondMismatch = i;
                    } else {
                        return false;
                    }
                }
            }

            if (secondMismatch == -1) {
                return firstMismatch != -1;
            }

            return secondMismatch == firstMismatch + 1
                    && first.charAt(firstMismatch)
                    == second.charAt(secondMismatch)
                    && first.charAt(secondMismatch)
                    == second.charAt(firstMismatch);
        }

        String longer =
                firstLength > secondLength ? first : second;

        String shorter =
                firstLength > secondLength ? second : first;

        int i = 0;
        int j = 0;

        boolean differenceFound = false;

        while (i < longer.length() && j < shorter.length()) {

            if (longer.charAt(i) == shorter.charAt(j)) {

                i++;
                j++;

            } else {

                if (differenceFound) {
                    return false;
                }

                differenceFound = true;

                i++;
            }
        }

        return true;
    }

    private boolean isNumeric(String token) {

        return token.chars().allMatch(Character::isDigit);
    }

    public record NormalizedQuery(
            String originalQuery,
            String cleanedQuery,
            String correctedQuery,
            List<String> originalTokens,
            List<String> correctedTokens
    ) {
    }
}
