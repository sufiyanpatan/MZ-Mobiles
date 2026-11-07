package mz.mobile.bussiness.search.api.controller;

import mz.mobile.bussiness.search.api.dto.ProductSearchResponse;
import mz.mobile.bussiness.search.api.service.ProductSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class ProductSearchController {

    private final ProductSearchService searchService;

    public ProductSearchController(
            ProductSearchService searchService
    ) {
        this.searchService = searchService;
    }

    /**
     * Searches products using the supplied query.
     */
    @GetMapping
    public ResponseEntity<?> searchProducts(

            @RequestParam("q") String query,

            @RequestParam(defaultValue = "10") int limit

    ) {

        try {

            ProductSearchResponse response =
                    searchService.search(query, limit);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            exception.getMessage()
                    )
            );
        }
    }

    /**
     * Checks database connectivity for search.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        boolean databaseAvailable =
                searchService.isDatabaseAvailable();

        Map<String, Object> response = Map.of(
                "status",
                databaseAvailable ? "UP" : "DOWN",

                "database",
                databaseAvailable ? "AVAILABLE" : "UNAVAILABLE"
        );

        return ResponseEntity
                .status(databaseAvailable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}