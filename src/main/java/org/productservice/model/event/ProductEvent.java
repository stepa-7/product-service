package org.productservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {
    private String eventId;
    private String eventType;
    private Long eventVersion;
    private Instant eventTimestamp;
    private ProductEventPayload product;
}
