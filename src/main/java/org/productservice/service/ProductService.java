package org.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.exception.ConflictException;
import org.productservice.exception.InvalidSortFieldException;
import org.productservice.exception.ProductNotFoundException;
import org.productservice.mapper.ProductMapper;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.ProductStatus;
import org.productservice.model.ProductsPageResponse;
import org.productservice.model.UpdateProductStatusResponse;
import org.productservice.model.entity.ProductEntity;
import org.productservice.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public void deleteProduct(UUID productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        productRepository.delete(entity);
        log.info("Товар успешно удален productId={}", productId);
    }

    public ProductDto getProductById(UUID id) {
        return productRepository.findById(id)
                .map(entity -> {
                    log.info("Товар успешно получен productId={}", entity.getId());
                    return productMapper.toDto(entity);
                })
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductsPageResponse getProducts(Integer page, Integer size, String sortBy, String direction, String name, String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice) {
        Set<String> ALLOWED = Set.of("name", "price", "createdAt", "updatedAt");
        if (sortBy == null || !ALLOWED.contains(sortBy)) {
            throw new InvalidSortFieldException(sortBy);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> pageProductDto = getFilteredProducts(pageable, name, category, status, minPrice, maxPrice);
        ProductsPageResponse response = new ProductsPageResponse();
        pageProductDto.forEach(response::addContentItem);
        response.setFirst(pageProductDto.isFirst());
        response.setLast(pageProductDto.isLast());
        response.setPage(pageProductDto.getNumber());
        response.setSize(pageProductDto.getSize());
        response.setTotalElements((int) pageProductDto.getTotalElements());
        response.setTotalPages(pageProductDto.getTotalPages());
        log.info("Товары успешно получены");
        return response;
    }

    private Page<ProductDto> getFilteredProducts(Pageable pageable, String name, String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<ProductEntity> spec = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }

        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category"), category));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        return productRepository.findAll(spec, pageable)
                .map(productMapper::toDto);
    }

    @Transactional
    public UpdateProductStatusResponse updateProductStatus(UUID id, ProductStatus status) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(status);
        productRepository.save(product);
        log.info("Статус товара обновлен productId={}", id);

        UpdateProductStatusResponse response = new UpdateProductStatusResponse();
        response.setId(id);
        response.setStatus(status);
        return response;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createFromEvent(ProductEventPayload productEventPayload) {
        UUID productId = productEventPayload.getId();

        if (productRepository.existsById(productId)) {
            throw new ConflictException("Товар уже существует: " + productId);
        }

        ProductEntity entity = productMapper.toEntityFromEvent(productEventPayload);
        entity.setUpdatedAt(Instant.now());

        try {
            productRepository.save(entity);
            log.info("Товар успешно создан productId={}", productId);
        }
        catch (DataIntegrityViolationException e) {
            log.info("Товар уже создан (race) productId={}", productId);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateFromEvent(ProductEventPayload productEventPayload) {
        UUID productId = productEventPayload.getId();

        ProductEntity product = productRepository.getProductEntityById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Long productVersion = product.getVersion() != null ? product.getVersion() : 0L;
        Long eventVersion = productEventPayload.getVersion();

        if (eventVersion == null || eventVersion <= productVersion) {
            log.info("Событие устаревшее eventVersion={} productVersion={}", eventVersion, productVersion);
            return;
        }
        productMapper.updateEntityFromEvent(productEventPayload, product);
        product.setUpdatedAt(Instant.now());

        productRepository.save(product);
        log.info("Товар успешно обновлен productId={}", productId);
    }
}
