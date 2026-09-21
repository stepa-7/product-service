package org.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.controller.ProductNotFoundException;
import org.productservice.model.entity.ProcessedEventEntity;
import org.productservice.model.entity.ProductEntity;
import org.productservice.model.event.ProductEvent;
import org.productservice.model.event.ProductEventPayload;
import org.productservice.model.event.ProductEventType;
import org.productservice.repository.ProcessedEventRepository;
import org.productservice.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventService {
    private final ProcessedEventRepository eventRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void process(ProductEvent event) {
        validateEvent(event);

        UUID eventId = event.getEventId();
        ProductEventType eventType = event.getEventType();

        log.info("Начата обработка события eventId={}", eventId);

        if (eventRepository.existsProcessedEventEntityByEventId(eventId)) {
            log.info("Событие уже обработано eventId={}", eventId);
            return;
        }

        if (eventType.equals(ProductEventType.PRODUCT_CREATED)) {
            handleCreate(event);
        } else if (eventType.equals(ProductEventType.PRODUCT_UPDATED)) {
            handleUpdate(event);
        }

        log.info("Событие успешно обработано eventId={}", eventId);

        try {
            eventRepository.save(ProcessedEventEntity.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Гонка по eventId={}, уже обработано", eventId);
        }
    }

    private void handleCreate(ProductEvent event) {
        UUID productId = event.getProduct().getId();
        ProductEventPayload productEventPayload = event.getProduct();

        if (!productRepository.existsById(productId)) { // TODO: mapper
            productRepository.save(ProductEntity.builder()
                    .id(productId)
                    .name(productEventPayload.getName())
                    .description(productEventPayload.getDescription())
                    .category(productEventPayload.getCategory())
                    .price(productEventPayload.getPrice())
                    .currency(productEventPayload.getCurrency())
                    .status(productEventPayload.getStatus())
                    .createdAt(productEventPayload.getCreatedAt())
                    .updatedAt(productEventPayload.getUpdatedAt())
                    .version(productEventPayload.getVersion())
                .build());

            log.info("Товар успешно создан productId={}", productId);
        } else {
            log.info("Товар уже создан productId={}", productId);
            return;
        }
    }

    private void handleUpdate(ProductEvent event) {
        UUID productId = event.getProduct().getId();
        ProductEventPayload productEventPayload = event.getProduct();

        Optional<ProductEntity> productEntityOptional = productRepository.getProductEntityById(productId);
        if (productEntityOptional.isPresent()) {
            ProductEntity product = productEntityOptional.get();

            Long productVersion = product.getVersion() != null ? product.getVersion() : 0L;
            Long eventVersion = productEventPayload.getVersion();

            if (eventVersion == null || eventVersion <= productVersion) {
                log.info("Событие устаревшее eventVersion={} productVersion={}", eventVersion, productVersion);
                return;
            }
            product.setName(productEventPayload.getName()); // TODO: mapper
            product.setDescription(productEventPayload.getDescription());
            product.setCategory(productEventPayload.getCategory());
            product.setPrice(productEventPayload.getPrice());
            product.setCurrency(productEventPayload.getCurrency());
            product.setStatus(productEventPayload.getStatus());
            product.setUpdatedAt(Instant.now());

            productRepository.save(product);
            log.info("Товар успешно обновлен productId={}", productId);
        } else {
            log.info("Товар не существует, для того, чтобы его обновить productId={}", productId);
            throw new ProductNotFoundException(productId);
        }
    }

    private void validateEvent(ProductEvent event) {
        if (
                event == null ||
                event.getEventId() == null ||
                event.getEventType() == null ||
                event.getEventVersion() == null ||
                event.getProduct() == null ||
                event.getProduct().getId() == null
        ) {
            throw new IllegalArgumentException("Неверное событие");
        }
    }

}
