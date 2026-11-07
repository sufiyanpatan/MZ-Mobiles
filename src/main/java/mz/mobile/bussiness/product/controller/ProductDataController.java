package mz.mobile.bussiness.product.controller;

import mz.mobile.bussiness.product.dto.ProductGenerationResponse;
import mz.mobile.bussiness.product.service.ProductDataGenerator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class ProductDataController {

    private final ProductDataGenerator productDataGenerator;

    public ProductDataController(
            ProductDataGenerator productDataGenerator) {

        this.productDataGenerator = productDataGenerator;
    }

    @PostMapping("/generate")
    public ResponseEntity<ProductGenerationResponse> generateProducts(
            @RequestParam(defaultValue = "100000") int count) {

        ProductGenerationResponse response =
                productDataGenerator.generateProducts(count);

        return ResponseEntity.ok(response);
    }
}