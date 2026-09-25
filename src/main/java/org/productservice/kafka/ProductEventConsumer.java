package org.productservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.config.properties.AppProperties;
import org.productservice.model.ProductEvent;
import org.productservice.model.ProductEventType;
import org.productservice.service.ProductEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {
    private final ProductEventService eventService;
    private final AppProperties appProperties;

    @KafkaListener(topics = "${app.product-consumer-properties.topic}", containerFactory = "productConsumerFactory")
    public void consume(List<ProductEvent> events, Acknowledgment ack) {
        for (ProductEvent event : events) {
            UUID eventId = event.getId();
            UUID productId = event.getProduct() != null ? event.getProduct().getId() : null;
            ProductEventType eventType = event.getEventType();

            log.info("Получено событие eventId={}, productId={}, eventType={}", eventId, productId, eventType);
            try {
                eventService.process(event);
            } catch (Exception ex) {
                log.error("Ошибка при обработке события eventId={}, productId={}, eventType={}, reason={}",
                        eventId, productId, eventType, ex.getMessage(), ex);
                throw ex;
            }
        }
        ack.acknowledge();
    }
}
