package mz.mobile.bussiness.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductGenerationResponse {

    private int requestedCount;

    private int insertedCount;

    private int batchSize;

    private long executionTimeMs;

    private String message;
}