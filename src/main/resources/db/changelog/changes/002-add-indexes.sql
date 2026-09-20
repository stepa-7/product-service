--liquibase formatted sql

--changeset schum:002-add-indexes
--comment: Создание индексов

CREATE INDEX idx_product_category on product(category);
--rollback DROP INDEX idx_product_category;

CREATE INDEX idx_product_status on product(status);
--rollback DROP INDEX idx_product_status;

CREATE INDEX idx_product_price on product(price);
--rollback DROP INDEX idx_product_price;

CREATE INDEX idx_product_created_at on product(created_at);
--rollback DROP INDEX idx_product_created_at;
