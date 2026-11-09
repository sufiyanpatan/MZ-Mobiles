
package mz.mobile.bussiness.search.index.controller;

import mz.mobile.bussiness.search.index.dto.SearchIndexBuildResponse;
import mz.mobile.bussiness.search.index.service.SearchIndexBuilderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/search/index")
public class SearchIndexController {

    private final SearchIndexBuilderService indexBuilderService;

    public SearchIndexController(
            SearchIndexBuilderService indexBuilderService
    ) {
        this.indexBuilderService = indexBuilderService;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<SearchIndexBuildResponse> rebuildIndex(

            @RequestParam(required = false) Long maxProducts,

            @RequestParam(defaultValue = "500") int batchSize

    ) {

        long maximum = maxProducts == null
                ? Long.MAX_VALUE
                : maxProducts;

        SearchIndexBuildResponse response =
                indexBuilderService.rebuildIndex(
                        maximum,
                        batchSize
                );

        return ResponseEntity.ok(response);
    }
}
