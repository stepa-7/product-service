package org.productservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.model.event.ProductEvent;
import org.productservice.model.event.ProductEventType;
import org.productservice.service.ProductEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {
    private final ProductEventService eventService;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "product-events", groupId = "product-service")
    public void consume(ProductEvent event, Acknowledgment ack) {
        UUID eventId = event.getEventId();
        UUID productId = event.getProduct() != null ? event.getProduct().getId() : null;
        ProductEventType eventType = event.getEventType();

        log.info("Получено событие eventId={}, productId={}, eventType={}", eventId, productId, eventType);
        try {
            eventService.process();
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Ошибка при обработке события eventId={}, productId={}, eventType={}, reason={}",
                    eventId, productId, eventType, ex.getMessage(), ex);
            throw ex;
        }
    }
}
