package org.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.productservice.exception.ProductNotFoundException;
import org.productservice.model.ProductEventPayload;
import org.productservice.model.entity.ProductEntity;
import org.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createFromEvent(ProductEventPayload productEventPayload) {
        UUID productId = productEventPayload.getId();

        if (!productRepository.existsById(productId)) { // TODO: mapper
            productRepository.save(ProductEntity.builder()
                    .id(productId)
                    .name(productEventPayload.getName())
                    .description(productEventPayload.getDescription())
                    .category(productEventPayload.getCategory())
                    .price(productEventPayload.getPrice())
                    .currency(productEventPayload.getCurrency())
                    .status(productEventPayload.getStatus())
                    .version(productEventPayload.getVersion())
                    .build());

            log.info("Товар успешно создан productId={}", productId);
        } else {
            log.info("Товар уже создан productId={}", productId);
            return;
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateFromEvent(ProductEventPayload productEventPayload) {
        UUID productId = productEventPayload.getId();

        Optional<ProductEntity> productEntityOptional = productRepository.getProductEntityById(productId);
        if (productEntityOptional.isPresent()) {
            ProductEntity product = productEntityOptional.get();

            Long productVersion = product.getVersion() != null ? product.getVersion() : 0L;
            Long eventVersion = productEventPayload.getVersion();

            if (eventVersion == null || eventVersion <= productVersion) {
                log.info("Событие устаревшее eventVersion={} productVersion={}", eventVersion, productVersion);
                return;
            }
            product.setName(productEventPayload.getName()); // TODO: mapper
            product.setDescription(productEventPayload.getDescription());
            product.setCategory(productEventPayload.getCategory());
            product.setPrice(productEventPayload.getPrice());
            product.setCurrency(productEventPayload.getCurrency());
            product.setStatus(productEventPayload.getStatus());
            product.setUpdatedAt(Instant.now());

            productRepository.save(product);
            log.info("Товар успешно обновлен productId={}", productId);
        } else {
            log.info("Товар не существует, для того, чтобы его обновить productId={}", productId);
            throw new ProductNotFoundException(productId);
        }
    }
}
