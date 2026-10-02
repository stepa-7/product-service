package org.productservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.productservice.model.ProductEvent;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.ProductEventType;
import org.productservice.model.ProductStatus;
import org.productservice.model.entity.ProcessedEventEntity;
import org.productservice.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventServiceTest {
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductEventService productEventService;

    @Test
    void processCreate() {
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
        ProductEvent event = new ProductEvent(id, ProductEventType.PRODUCT_CREATED, 1L, OffsetDateTime.now(), payload);

        when(processedEventRepository.existsProcessedEventEntityByEventId(id)).thenReturn(false);

        productEventService.process(event);

        verify(productService).createFromEvent(payload);
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void processUpdate() {
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
        ProductEvent event = new ProductEvent(id, ProductEventType.PRODUCT_UPDATED, 1L, OffsetDateTime.now(), payload);

        when(processedEventRepository.existsProcessedEventEntityByEventId(id)).thenReturn(false);

        productEventService.process(event);

        verify(productService).updateFromEvent(event.getProduct());
        verify(productService, never()).createFromEvent(any());
    }
}