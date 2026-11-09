
package mz.mobile.bussiness.product.repository;

import mz.mobile.bussiness.product.dto.ProductDetailsResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductCatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * These are the columns that the frontend needs.
     *
     * We use your existing products table.
     */
    private static final String SELECT_COLUMNS = """
            SELECT
                id,
                sku,
                category,
                brand,
                product_name,
                model,
                description,
                price,
                condition_grade
            FROM products
            """;

    /*
     * Converts a database row into our DTO.
     */
    private static final RowMapper<ProductDetailsResponse> PRODUCT_MAPPER =
            (rs, rowNum) -> {

                String category = rs.getString("category");

                String productName = rs.getString("product_name");

                // Keep your current hardcoded illustration style.
                String emoji = isLaptop(category, productName)
                        ? "💻"
                        : "📱";

                return new ProductDetailsResponse(

                        rs.getLong("id"),

                        rs.getString("sku"),

                        category,

                        rs.getString("brand"),

                        productName,

                        rs.getString("model"),

                        rs.getString("description"),

                        rs.getBigDecimal("price") == null
                                ? java.math.BigDecimal.ZERO
                                : rs.getBigDecimal("price"),

                        rs.getString("condition_grade"),

                        emoji
                );
            };

    /*
     * Get a limited number of products for the homepage.
     */
    public List<ProductDetailsResponse> findFeatured(int limit) {

        String sql = SELECT_COLUMNS
                + " ORDER BY id DESC LIMIT ?";

        return jdbcTemplate.query(
                sql,
                PRODUCT_MAPPER,
                limit
        );
    }

    /*
     * Retrieve a particular product using its ID.
     */
    public Optional<ProductDetailsResponse> findById(long id) {

        String sql = SELECT_COLUMNS
                + " WHERE id = ? LIMIT 1";

        List<ProductDetailsResponse> products =
                jdbcTemplate.query(
                        sql,
                        PRODUCT_MAPPER,
                        id
                );

        return products.stream().findFirst();
    }

    /*
     * Decide whether to display mobile or laptop illustration.
     */
    private static boolean isLaptop(
            String category,
            String productName
    ) {

        return (category != null
                && category.toLowerCase().contains("laptop"))

                || (productName != null
                && productName.toLowerCase().contains("macbook"));
    }
}
