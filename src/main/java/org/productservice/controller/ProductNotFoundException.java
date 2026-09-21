package org.productservice.controller;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
        super("Товар с ID " + id.toString() + " не найден");
    }
}
