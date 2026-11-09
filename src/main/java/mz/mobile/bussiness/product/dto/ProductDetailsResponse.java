
package mz.mobile.bussiness.product.dto;

import java.math.BigDecimal;

/**
 * This DTO defines the product information
 * that the frontend is allowed to receive.
 */
public record ProductDetailsResponse(

        Long id,

        String sku,

        String category,

        String brand,

        String name,

        String model,

        String description,

        BigDecimal price,

        String condition,

        String emoji

) {
}
