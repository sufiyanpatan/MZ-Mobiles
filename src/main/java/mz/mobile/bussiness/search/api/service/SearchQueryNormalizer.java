package mz.mobile.bussiness.search.api.service;


//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.Collections;
//import java.util.stream.Collectors;
//
//@Component
//public class SearchQueryNormalizer {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    public SearchQueryNormalizer(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public NormalizedQuery normalize(String originalQuery) {
//
//        if (originalQuery == null || originalQuery.isBlank()) {
//            throw new IllegalArgumentException(
//                    "Search query cannot be empty"
//            );
//        }
//
//        if (originalQuery.length() > 100) {
//            throw new IllegalArgumentException(
//                    "Search query cannot exceed 100 characters"
//            );
//        }
//
//        // Step 1: Clean the input
//        String cleanedQuery = cleanQuery(originalQuery);
//
//        if (cleanedQuery.isBlank()) {
//            throw new IllegalArgumentException(
//                    "Search query must contain letters or numbers"
//            );
//        }
//
//        // Step 2: Split query into individual words
//        List<String> originalTokens = Arrays.asList(
//                cleanedQuery.split("\\s+")
//        );
//
//        // Step 3: Find known spelling corrections
//        Map<String, String> aliases =
//                findAliases(originalTokens);
//
//        // Step 4: Replace misspelled words
//        List<String> correctedTokens = originalTokens.stream()
//                .map(token -> aliases.getOrDefault(token, token))
//                .toList();
//
//        String correctedQuery = String.join(
//                " ",
//                correctedTokens
//        );
//
//        return new NormalizedQuery(
//                originalQuery,
//                cleanedQuery,
//                correctedQuery,
//                originalTokens,
//                correctedTokens
//        );
//    }
//
//    /**
//     * Converts input into lowercase and removes
//     * unnecessary special characters.
//     */
//    private String cleanQuery(String query) {
//
//        return query
//                .toLowerCase(Locale.ROOT)
//                .replaceAll("[^\\p{L}\\p{N}]+", " ")
//                .trim()
//                .replaceAll("\\s+", " ");
//    }
//
//    /**
//     * Finds matching aliases from MySQL.
//     *
//     * Example:
//     * macbok -> macbook
//     * iphne  -> iphone
//     */
//    private Map<String, String> findAliases(
//            List<String> tokens) {
//
//        if (tokens.isEmpty()) {
//            return Collections.emptyMap();
//        }
//
//        // Remove duplicate words before querying MySQL
//        List<String> uniqueTokens = tokens.stream()
//                .distinct()
//                .toList();
//
//        String placeholders = uniqueTokens.stream()
//                .map(token -> "?")
//                .collect(Collectors.joining(", "));
//
//        String sql = """
//                SELECT
//                    a.alias_term,
//                    v.term AS canonical_term
//                FROM search_alias a
//                JOIN search_vocabulary v
//                    ON a.canonical_term_id = v.term_id
//                WHERE a.alias_term IN (
//                """ + placeholders + ")";
//
//        Map<String, String> aliases = new HashMap<>();
//
//        jdbcTemplate.query(
//                sql,
//                rs -> {
//                    aliases.put(
//                            rs.getString("alias_term"),
//                            rs.getString("canonical_term")
//                    );
//                },
//                uniqueTokens.toArray()
//        );
//
//        return aliases;
//    }
//
//    /**
//     * Holds original and corrected search information.
//     */
//    public record NormalizedQuery(
//            String originalQuery,
//            String cleanedQuery,
//            String correctedQuery,
//            List<String> originalTokens,
//            List<String> correctedTokens
//    ) {
//    }
//}

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

        // Step 1: Clean the original query.
        String cleanedQuery = cleanQuery(originalQuery);

        if (cleanedQuery.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must contain letters or numbers"
            );
        }

        // Step 2: Split the query into individual words.
        List<String> originalTokens =
                Arrays.asList(cleanedQuery.split("\\s+"));

        if (originalTokens.size() > MAX_QUERY_TOKENS) {
            throw new IllegalArgumentException(
                    "Search query cannot contain more than 8 words"
            );
        }

        // Step 3: Look for manually configured aliases.
        Map<String, String> aliases = findAliases(
                originalTokens.stream().distinct().toList()
        );

        // Step 4: Correct tokens and remove duplicates.
        Set<String> correctedTokens = new LinkedHashSet<>();

        for (String token : originalTokens) {

            String correctedToken = aliases.get(token);

            if (correctedToken == null) {
                correctedToken = correctUsingVocabulary(token);
            }

            correctedTokens.add(correctedToken);
        }

        List<String> finalTokens =
                List.copyOf(correctedTokens);

        String correctedQuery =
                String.join(" ", finalTokens);

        return new NormalizedQuery(
                originalQuery,
                cleanedQuery,
                correctedQuery,
                originalTokens,
                finalTokens
        );
    }

    /**
     * Converts the query to lowercase and removes
     * special characters.
     */
    private String cleanQuery(String query) {

        return query.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Finds manually configured corrections from search_alias.
     */
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

    /**
     * First checks whether the word exists in the vocabulary.
     * If it does not, tries to find a similar word.
     */
    private String correctUsingVocabulary(String token) {

        // Avoid fuzzy correction for short words and numbers.
        if (token.length() < 4 || isNumeric(token)) {
            return token;
        }

        boolean exactMatch = vocabularyTermExists(token);

        if (exactMatch) {
            return token;
        }

        return findFuzzyCorrection(token)
                .orElse(token);
    }

    /**
     * Checks whether the exact word exists.
     */
    private boolean vocabularyTermExists(String token) {

//        Boolean exists = jdbcTemplate.queryForObject(
//                """
//                SELECT 1
//                FROM search_vocabulary
//                WHERE term = ?
//                LIMIT 1
//                """,
//                rs -> rs.next(),
//                token
//        );
//
//        return Boolean.TRUE.equals(exists);

        Integer exists = jdbcTemplate.queryForObject(
                """
                SELECT 1
                FROM search_vocabulary
                WHERE term = ?
                LIMIT 1
                """,
                Integer.class,
                token
        );
        return exists != null;
    }

    /**
     * Finds possible spelling corrections.
     *
     * Uses first_char and term_length to reduce
     * the number of vocabulary records Java checks.
     */
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

    /**
     * Supports one insertion, deletion, substitution,
     * or adjacent-character transposition.
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

        // Same length: substitution or adjacent transposition.
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

            // No mismatch or one substitution.
            if (secondMismatch == -1) {
                return firstMismatch != -1;
            }

            // Check whether the two different characters
            // are swapped next to each other.
            return secondMismatch == firstMismatch + 1
                    && first.charAt(firstMismatch)
                    == second.charAt(secondMismatch)
                    && first.charAt(secondMismatch)
                    == second.charAt(firstMismatch);
        }

        // Different lengths: one insertion or deletion.
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

        return token.chars()
                .allMatch(Character::isDigit);
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