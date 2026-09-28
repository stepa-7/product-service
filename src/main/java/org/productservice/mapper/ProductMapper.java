package org.productservice.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.productservice.model.ProductDto;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.entity.ProductEntity;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(ProductEntity entity);
    ProductEntity toEntity(ProductDto productDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromEvent(ProductEventPayload payload, @MappingTarget ProductEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntityFromEvent(ProductEventPayload payload);
}