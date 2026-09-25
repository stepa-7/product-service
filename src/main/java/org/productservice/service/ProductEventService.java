package org.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.model.entity.ProcessedEventEntity;
import org.productservice.model.ProductEvent;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.ProductEventType;
import org.productservice.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventService {
    private final ProcessedEventRepository eventRepository;
    private final ProductService productService;

    @Transactional
    public void process(ProductEvent event) {
        validateEvent(event);

        UUID eventId = event.getId();
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
                    .eventId(event.getId())
                    .eventType(event.getEventType())
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Гонка по eventId={}, уже обработано", eventId);
        }
    }

    private void handleCreate(ProductEvent event) {
        ProductEventPayload productEventPayload = event.getProduct();
        productService.createFromEvent(productEventPayload);
    }

    private void handleUpdate(ProductEvent event) {
        ProductEventPayload productEventPayload = event.getProduct();
        productService.updateFromEvent(productEventPayload);
    }

    private void validateEvent(ProductEvent event) {
        if (
                event == null ||
                event.getId() == null ||
                event.getEventType() == null ||
                event.getEventVersion() == null ||
                event.getProduct() == null ||
                event.getProduct().getId() == null
        ) {
            throw new IllegalArgumentException("Неверное событие");
        }
    }

}
