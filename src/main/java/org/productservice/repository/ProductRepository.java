package org.productservice.repository;

import org.productservice.model.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    @Override
    boolean existsById(UUID uuid);

    Optional<ProductEntity> getProductEntityById(UUID id);

}
