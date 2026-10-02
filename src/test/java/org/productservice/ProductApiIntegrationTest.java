package org.productservice;


import org.junit.jupiter.api.Test;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductEvent;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.ProductEventType;
import org.productservice.model.ProductStatus;
import org.productservice.repository.ProcessedEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductApiIntegrationTest extends IntegrationTestBase {
    private static final String TOPIC = "product-events";

    @Autowired
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void createProductTest() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ProductEventPayload payload = new ProductEventPayload(
                productId,
                "MacBook Pro 14",
                "Laptop Apple",
                "LAPTOP",
                BigDecimal.valueOf(1250.50),
                "EUR",
                ProductStatus.ACTIVE,
                1L);

        ProductEvent event = new ProductEvent(
                eventId,
                ProductEventType.PRODUCT_CREATED,
                1L,
                OffsetDateTime.now(),
                payload);

        kafkaTemplate.send(TOPIC, productId.toString(), event).get();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var response = rest.getForEntity(
                            "/products/" + productId,
                            String.class);

                    System.out.println("Status: " + response.getStatusCode());
                    System.out.println("Body: " + response.getBody());

                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                });

        var response = rest.getForEntity(
                "/products/" + productId,
                ProductDto.class);
        ProductDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("MacBook Pro 14");
        assertThat(dto.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1250.50));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void idempotencyTest() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ProductEventPayload payload = new ProductEventPayload(
                productId,
                "MacBook Pro 14",
                "Laptop Apple",
                "LAPTOP",
                BigDecimal.valueOf(1250.50),
                "EUR",
                ProductStatus.ACTIVE,
                1L);

        ProductEvent event = new ProductEvent(
                eventId,
                ProductEventType.PRODUCT_CREATED,
                1L,
                OffsetDateTime.now(),
                payload);

        kafkaTemplate.send(TOPIC, productId.toString(), event).get();
        kafkaTemplate.send(TOPIC, productId.toString(), event).get();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var response = rest.getForEntity(
                            "/products/" + productId,
                            String.class);

                    System.out.println("Status: " + response.getStatusCode());
                    System.out.println("Body: " + response.getBody());

                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                });

        long count = processedEventRepository.countByEventId(eventId);
        assertThat(count).isEqualTo(1);

        var response = rest.getForEntity("/products/" + productId, String.class);
        assertThat(response.getBody()).contains("\"version\":1");
    }






    @Test
    void shouldNotUpdateProduct_whenEventVersionIsOlder() throws Exception {
        UUID productId = UUID.randomUUID();

        // 1. Создаём (payload.version = 1) → entity.version = 1
        sendEvent(buildCreatedEvent(UUID.randomUUID(), productId, 1L));
        awaitProductExists(productId);

        // 2. Обновляем (payload.version = 2) → entity.version = 2
        sendEvent(buildUpdatedEvent(UUID.randomUUID(), productId, 2L, "First update"));
        awaitProductName(productId, "First update");

        // 3. Устаревшее (payload.version = 1) → 1 <= 2 → пропуск
        sendEvent(buildUpdatedEvent(UUID.randomUUID(), productId, 1L, "Stale name"));
        Thread.sleep(500);

        var response = rest.getForEntity("/products/" + productId, String.class);
        assertThat(response.getBody()).contains("\"name\":\"First update\"");
        assertThat(response.getBody()).doesNotContain("Stale name");
        assertThat(response.getBody()).contains("\"version\":2");
    }

    @Test
    void shouldReturn404_whenProductNotFound() {
        UUID randomId = UUID.randomUUID();

        var response = rest.getForEntity("/products/" + randomId, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("\"error\":\"PRODUCT_NOT_FOUND\"");
        assertThat(response.getBody()).contains(randomId.toString());
    }

    @Test
    void shouldFilterByNameAndPrice() throws Exception {
        // given — три товара
        UUID macbookId = UUID.randomUUID();
        UUID macbookAirId = UUID.randomUUID();
        UUID iphoneId = UUID.randomUUID();

        sendEvent(buildCreatedEvent(UUID.randomUUID(), macbookId, "MacBook Pro", 1500, 1L));
        sendEvent(buildCreatedEvent(UUID.randomUUID(), macbookAirId, "MacBook Air", 900, 1L));
        sendEvent(buildCreatedEvent(UUID.randomUUID(), iphoneId, "iPhone 15", 1200, 1L));

        awaitProductExists(macbookId);
        awaitProductExists(macbookAirId);
        awaitProductExists(iphoneId);

        // when — name=macbook (регистронезависимо) + minPrice=1000
        var response = rest.getForEntity(
                "/products?name=macbook&minPrice=1000",
                String.class);

        // then — только MacBook Pro (1500), не Air (900), не iPhone
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("MacBook Pro");
        assertThat(response.getBody()).doesNotContain("MacBook Air");
        assertThat(response.getBody()).doesNotContain("iPhone 15");
    }

    @Test
    void shouldDeleteProduct_whenDeleteCalled() throws Exception {
        // given — создаём товар
        UUID productId = UUID.randomUUID();
        sendEvent(buildCreatedEvent(UUID.randomUUID(), productId, 1L));
        awaitProductExists(productId);

        // when — удаляем
        var deleteResponse = rest.exchange(
                "/products/" + productId,
                HttpMethod.DELETE,
                null,
                Void.class);

        // then — 204
        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(204);

        // then — повторный GET → 404
        var getResponse = rest.getForEntity("/products/" + productId, String.class);
        assertThat(getResponse.getStatusCode().value()).isEqualTo(404);
        assertThat(getResponse.getBody()).contains("\"error\":\"PRODUCT_NOT_FOUND\"");
    }

    @Test
    void shouldReturn404_whenDeleteNonExistentProduct() {
        UUID randomId = UUID.randomUUID();

        var response = rest.exchange(
                "/products/" + randomId,
                HttpMethod.DELETE,
                null,
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldUpdateProduct_whenEventVersionIsNewer() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        sendEvent(buildCreatedEvent(UUID.randomUUID(), productId, 1L));
        awaitProductExists(productId);

        // when
        ProductEvent updateEvent = buildEvent(
                UUID.randomUUID(),
                productId,
                ProductEventType.PRODUCT_UPDATED,
                "MacBook Pro 16",
                BigDecimal.valueOf(2499.99),
                2L);
        sendEvent(updateEvent);

        // then
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var response = rest.getForEntity("/products/" + productId, String.class);
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                    assertThat(response.getBody()).contains("\"name\":\"MacBook Pro 16\"");
                    assertThat(response.getBody()).contains("\"price\":2499.99");
                    assertThat(response.getBody()).contains("\"version\":2");
                });
    }

    private void sendEvent(ProductEvent event) throws Exception {
        kafkaTemplate.send(TOPIC, event.getProduct().getId().toString(), event).get();
    }

    private ProductEvent buildCreatedEvent(UUID eventId, UUID productId, long eventVersion) {
        return buildEvent(eventId, productId, ProductEventType.PRODUCT_CREATED,
                "MacBook Pro 14", BigDecimal.valueOf(1250.50), eventVersion);
    }

    private ProductEvent buildCreatedEvent(UUID eventId, UUID productId, String name,
                                           int price, long eventVersion) {
        return buildEvent(eventId, productId, ProductEventType.PRODUCT_CREATED,
                name, BigDecimal.valueOf(price), eventVersion);
    }

    private ProductEvent buildUpdatedEvent(UUID eventId, UUID productId,
                                           long eventVersion, String newName) {
        return buildEvent(eventId, productId, ProductEventType.PRODUCT_UPDATED,
                newName, BigDecimal.valueOf(1250.50), eventVersion);
    }

    private ProductEvent buildEvent(UUID eventId, UUID productId, ProductEventType type,
                                    String name, BigDecimal price, long eventVersion) {
        ProductEventPayload payload = new ProductEventPayload(
                productId,
                name,
                "Laptop Apple",
                "LAPTOP",
                price,
                "EUR",
                ProductStatus.ACTIVE,
                eventVersion);

        return new ProductEvent(
                eventId,
                type,
                eventVersion,
                OffsetDateTime.now(),
                payload);
    }

    private void awaitProductExists(UUID productId) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var response = rest.getForEntity("/products/" + productId, String.class);
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                });
    }

    private void awaitProductName(UUID productId, String expectedName) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var response = rest.getForEntity("/products/" + productId, String.class);
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                    assertThat(response.getBody()).contains("\"name\":\"" + expectedName + "\"");
                });
    }
}
