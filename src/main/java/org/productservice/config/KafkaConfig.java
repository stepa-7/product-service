package org.productservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.productservice.config.properties.AppProperties;
import org.productservice.model.event.ProductEvent;
import org.springdoc.core.converters.JsonNullableSupportConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.product-consumer-properties",
    name = "enabled",
    havingValue = "true")
public class KafkaConfig {
    private static final String TRUSTED_PACKAGES = "org.productservice.*";

    private final AppProperties appProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, ProductEvent> productConsumerFactory() { // TODO: DLT
        AppProperties.ProductConsumerProperties consumerProps = appProperties.getProductConsumerProperties();

        ConsumerFactory<UUID, ProductEvent> consumerFactory = buildConsumerFactory(
                consumerProps.getGroupId(),
                consumerProps.getConsumerMaxPollRecords(),
                ProductEvent.class);

        ConcurrentKafkaListenerContainerFactory<UUID, ProductEvent> factory =  new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(consumerProps.getConsumerCount());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    private <T> ConsumerFactory<UUID, T> buildConsumerFactory(String groupId,
                                                                                       int maxPollRecords,
                                                                                       Class<T> eventClass) {
        Map<String, Object> props = new HashMap<>();

        props.put(GROUP_ID_CONFIG, groupId);
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<T>(eventClass, false);
        jsonDeserializer.addTrustedPackages(TRUSTED_PACKAGES);
        ErrorHandlingDeserializer<T> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(props, new UUIDDeserializer(), errorHandlingDeserializer);
    }
}
