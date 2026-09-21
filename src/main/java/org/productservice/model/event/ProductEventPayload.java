package org.productservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.productservice.model.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductEventPayload {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String currency;
    private ProductStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
