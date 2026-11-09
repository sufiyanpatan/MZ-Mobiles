package mz.mobile.bussiness.product.service;

import mz.mobile.bussiness.product.dto.ProductGenerationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductDataGenerator {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_SQL = """
            INSERT INTO products
            (
                sku,
                category,
                brand,
                product_name,
                model,
                description,
                price,
                condition_grade
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String[] MOBILE_BRANDS = {
            "Apple", "Samsung", "OnePlus",
            "Xiaomi", "Vivo", "Oppo", "Realme"
    };

    private static final String[] LAPTOP_BRANDS = {
            "Apple", "Dell", "HP",
            "Lenovo", "Asus", "Acer"
    };

    private static final String[] COLORS = {
            "Black", "White", "Blue",
            "Silver", "Gold", "Grey"
    };

    private static final String[] CONDITIONS = {
            "EXCELLENT", "GOOD", "FAIR"
    };

    private static final String[] STORAGE = {
            "64GB", "128GB", "256GB", "512GB"
    };

    private static final String[] RAM = {
            "8GB RAM", "16GB RAM", "32GB RAM"
    };

    private static final String[] LAPTOP_MODELS = {
            "Inspiron 15", "Latitude 5420",
            "XPS 13", "Pavilion 15",
            "ThinkPad E14", "IdeaPad Slim 5",
            "MacBook Air M1", "MacBook Air M2",
            "MacBook Pro M3", "VivoBook 15",
            "Aspire 5"
    };

    private static final String[] IPHONE_MODELS = {
            "iPhone X", "iPhone XR",
            "iPhone 11", "iPhone 12",
            "iPhone 13", "iPhone 14",
            "iPhone 15", "iPhone 16"
    };

    public ProductDataGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductGenerationResponse generateProducts(int count) {

        if (count < 1 || count > 1_000_00) {
            throw new IllegalArgumentException(
                    "Count must be between 1 and 100000"
            );
        }

        long startTime = System.currentTimeMillis();

        String batchPrefix = "DEMO-" + System.currentTimeMillis() + "-";

        int insertedCount = 0;

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 1; i <= count; i++) {

            Object[] product = generateProduct(
                    batchPrefix + i
            );

            batch.add(product);

            if (batch.size() == BATCH_SIZE) {

                jdbcTemplate.batchUpdate(
                        INSERT_SQL,
                        batch
                );

                insertedCount += batch.size();

                batch.clear();
            }
        }

        // Insert remaining records
        if (!batch.isEmpty()) {

            jdbcTemplate.batchUpdate(
                    INSERT_SQL,
                    batch
            );

            insertedCount += batch.size();
        }

        long executionTime =
                System.currentTimeMillis() - startTime;

        return new ProductGenerationResponse(
                count,
                insertedCount,
                BATCH_SIZE,
                executionTime,
                "Products generated successfully"
        );
    }

    private Object[] generateProduct(String sku) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        boolean isMobile = random.nextBoolean();

        String category;
        String brand;
        String productName;
        String model;
        String description;
        BigDecimal price;

        if (isMobile) {

            category = "MOBILE";

            brand = randomBrand(MOBILE_BRANDS);

            if (brand.equals("Apple")) {

                productName = randomElement(IPHONE_MODELS);

            } else if (brand.equals("Samsung")) {

                productName = "Galaxy S" +
                        random.nextInt(20, 26);

            } else if (brand.equals("OnePlus")) {

                productName = "OnePlus " +
                        random.nextInt(9, 16);

            } else {

                productName = brand + " " +
                        random.nextInt(10, 100);
            }

            model = productName.replace(" ", "-");

            description = productName + " " +
                    randomElement(STORAGE) + " " +
                    randomElement(COLORS) +
                    " Smartphone";

            price = BigDecimal.valueOf(
                    random.nextInt(5000, 150001)
            );

        } else {
            category = "LAPTOP";
            brand = randomBrand(LAPTOP_BRANDS);
            String laptopModel =
                    randomElement(LAPTOP_MODELS);
            productName = brand + " " + laptopModel;
            model = laptopModel.replace(" ", "-");

            description = productName + " " +
                    randomElement(RAM) + " " +
                    randomElement(STORAGE) +
                    " Laptop";

            price = BigDecimal.valueOf(
                    random.nextInt(20000, 200001)
            );
        }

        String condition = randomElement(CONDITIONS);

        return new Object[]{
                sku,
                category,
                brand,
                productName,
                model,
                description,
                price,
                condition
        };
    }

    private String randomBrand(String[] brands) {
        return randomElement(brands);
    }

    private String randomElement(String[] values) {
        return values[
                ThreadLocalRandom.current()
                        .nextInt(values.length)
                ];
    }
}