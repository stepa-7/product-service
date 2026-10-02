package org.productservice.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.entity.ProductEntity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(ProductEntity entity);
    ProductEntity toEntity(ProductDto productDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromEvent(ProductEventPayload payload, @MappingTarget ProductEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntityFromEvent(ProductEventPayload payload);

    default OffsetDateTime instantToOffset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    default Instant offsetToInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
}