
package mz.mobile.bussiness.product.service;

import mz.mobile.bussiness.product.dto.ProductDetailsResponse;
import mz.mobile.bussiness.product.repository.ProductCatalogRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductCatalogService {

    private static final int MAX_FEATURED_PRODUCTS = 24;

    private final ProductCatalogRepository repository;

    public ProductCatalogService(
            ProductCatalogRepository repository
    ) {
        this.repository = repository;
    }

    /*
     * Homepage product loading.
     */
    public List<ProductDetailsResponse> getFeatured(
            int requestedLimit
    ) {

        if (requestedLimit < 1) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be at least 1"
            );
        }

        // Never allow the frontend to request unlimited products.
        int safeLimit = Math.min(
                requestedLimit,
                MAX_FEATURED_PRODUCTS
        );

        return repository.findFeatured(safeLimit);
    }

    /*
     * Fetch one product's complete details.
     */
    public ProductDetailsResponse getById(long id) {

        if (id <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product ID must be positive"
            );
        }

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }
}
