package org.productservice.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@NoArgsConstructor
@Validated
@ConfigurationProperties("app")
public class AppProperties {

    @Valid
    @NotNull
    private KafkaProperties kafka = new KafkaProperties();

    @Data
    public static class KafkaProperties {
        @NotNull
        private Boolean logPayload;
    }

    @Valid
    @NotNull
    private ProductConsumerProperties productConsumerProperties= new ProductConsumerProperties();

    @Data
    public static class ProductConsumerProperties {
        @NotNull
        private Boolean enabled;

        @NotBlank
        private String topic;

        @NotBlank
        private String groupId;

        @NotNull
        private Integer consumerCount;

        @NotNull
        private Integer consumerMaxPollRecords;
    }
}
