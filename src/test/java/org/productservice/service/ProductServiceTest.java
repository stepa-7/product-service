package org.productservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.productservice.exception.ProductNotFoundException;
import org.productservice.mapper.ProductMapper;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.ProductStatus;
import org.productservice.model.ProductsPageResponse;
import org.productservice.model.entity.ProductEntity;
import org.productservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void createFromEvent() {
        UUID id = UUID.randomUUID();
        ProductEventPayload payload = new ProductEventPayload(
                id,
                "MacBook Pro 14",
                "Laptop Apple",
                "LAPTOP",
                BigDecimal.valueOf(100),
                "USD",
                ProductStatus.ACTIVE,
                0L
        );

        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("MacBook Pro 14")
                .build();

        when(productRepository.existsById(id)).thenReturn(false);
        when(productMapper.toEntityFromEvent(payload)).thenReturn(entity);

        productService.createFromEvent(payload);

        verify(productRepository).save(entity);
    }

    @Test
    void updateFromEvent() {
        UUID id = UUID.randomUUID();
        ProductEventPayload payload = new ProductEventPayload(
                id,
                "MacBook Pro 14",
                "Laptop Apple",
                "LAPTOP",
                BigDecimal.valueOf(100),
                "USD",
                ProductStatus.ACTIVE,
                1L
        );

        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("MacBook Pro 15")
                .version(0L)
                .build();

        when(productRepository.getProductEntityById(id)).thenReturn(Optional.of(entity));

        productService.updateFromEvent(payload);

        verify(productMapper).updateEntityFromEvent(payload, entity);
        verify(productRepository).save(entity);
    }

    @Test
    void updateProductStatus() {
        UUID id = UUID.randomUUID();
        ProductStatus status = ProductStatus.INACTIVE;

        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("MacBook Pro 14")
                .status(ProductStatus.ACTIVE)
                .version(0L)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));

        productService.updateProductStatus(id, status);

        assertEquals(ProductStatus.INACTIVE, entity.getStatus());
        verify(productRepository).save(entity);
    }

    @Test
    void updateProductStatusThrowsProductNotFound() {
        UUID id = UUID.randomUUID();
        ProductStatus status = ProductStatus.INACTIVE;

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProductStatus(id, status));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("MacBook Pro 14")
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));

        productService.deleteProduct(id);

        verify(productRepository).delete(entity);
    }

    @Test
    void deleteProductThrowProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(id));

        verify(productRepository, never()).delete((ProductEntity) any());
    }

    @Test
    void getProductById() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("MacBook Pro 14")
                .build();

        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName("MacBook Pro 14");

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productMapper.toDto(entity)).thenReturn(dto);

        ProductDto result = productService.getProductById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("MacBook Pro 14");

        verify(productMapper).toDto(entity);
    }

    @Test
    void getProducts() {
        ProductEntity entity = ProductEntity.builder()
                .id(UUID.randomUUID())
                .name("MacBook Pro 14")
                .build();

        ProductDto dto = new ProductDto();
        dto.setId(entity.getId());
        dto.setName("MacBook Pro 14");

        Page<ProductEntity> entityPage = new PageImpl<>(
                List.of(entity),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(productMapper.toDto(entity)).thenReturn(dto);

        ProductsPageResponse response = productService.getProducts(
                0, 20, "createdAt", "desc",
                null, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("MacBook Pro 14");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
