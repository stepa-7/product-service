package org.productservice.controller;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.productservice.api.ProductsApi;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductStatus;
import org.productservice.model.ProductsPageResponse;
import org.productservice.model.UpdateProductStatusRequest;
import org.productservice.model.UpdateProductStatusResponse;
import org.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductsController implements ProductsApi {
    private final ProductService productService;

    @Override
    public ResponseEntity<Void> deleteProductById(UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProductDto> getProductById(UUID id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Override
    public ResponseEntity<ProductsPageResponse> getProducts(Integer page, Integer size, String sort, String direction, @Nullable String name, @Nullable String category, @Nullable ProductStatus status, @Nullable BigDecimal minPrice, @Nullable BigDecimal maxPrice) {
        ProductsPageResponse response = productService.getProducts(page, size, sort, direction, name, category, status, minPrice, maxPrice);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UpdateProductStatusResponse> updateProductStatus(UUID id, UpdateProductStatusRequest updateProductStatusRequest) {
        UpdateProductStatusResponse response = productService.updateProductStatus(id, updateProductStatusRequest.getStatus());
        return ResponseEntity.ok(response);
    }
}
