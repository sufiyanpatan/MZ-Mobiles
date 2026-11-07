
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
            SearchIndexBuilderService indexBuilderService) {

        this.indexBuilderService = indexBuilderService;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<SearchIndexBuildResponse> rebuildIndex(
            @RequestParam(defaultValue = "1000000") long maxProducts,
            @RequestParam(defaultValue = "500") int batchSize
    ) {
        SearchIndexBuildResponse response =
                indexBuilderService.rebuildIndex(
                        maxProducts,
                        batchSize
                );

        return ResponseEntity.ok(response);
    }
}
