package mz.mobile.bussiness.product.controller;

import mz.mobile.bussiness.product.dto.ProductDetailsResponse;
import mz.mobile.bussiness.product.service.ProductCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    public ProductCatalogController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /*
     * GET /api/products/featured?limit=12
     *
     * Returns homepage products.
     */
    @GetMapping("/featured")
    public List<ProductDetailsResponse> getFeatured(

            @RequestParam(defaultValue = "12") int limit

    ) {

        return productCatalogService.getFeatured(limit);
    }

    /*
     * GET /api/products/101
     *
     * Returns complete details of one product.
     */
    @GetMapping("/{id}")
    public ProductDetailsResponse getById(

            @PathVariable long id

    ) {

        return productCatalogService.getById(id);
    }
}
