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
@ConfigurationProperties("app.infra")
public class InfraProperties {
    @Valid
    @NotNull
    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {
        @NotBlank
        private String bootstrapServers;
    }
}
