--liquibase formatted sql

--changeset schum:001-create-product
--comment: Создание таблицы товаров
CREATE TABLE product (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   name VARCHAR(255) NOT NULL CHECK (length(name) >= 1),
   description VARCHAR(255),
   category VARCHAR(255),
   price DECIMAL(19, 2) NOT NULL CHECK (price >= 0),
   currency VARCHAR(255) NOT NULL,
   status VARCHAR(20) NOT NULL CHECK (status in ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),
   created_at TIMESTAMPTZ,
   updated_at TIMESTAMPTZ,
   version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE product IS 'Таблица товаров';
COMMENT ON COLUMN product.id IS 'Уникальный идентификатор';
COMMENT ON COLUMN product.name IS 'Название товара';
COMMENT ON COLUMN product.description IS 'Описание товара';
COMMENT ON COLUMN product.category IS 'Категория';
COMMENT ON COLUMN product.price IS 'Цена';
COMMENT ON COLUMN product.currency IS 'Валюта, например EUR';
COMMENT ON COLUMN product.status IS 'Статус товара';
COMMENT ON COLUMN product.created_at IS 'Дата создания';
COMMENT ON COLUMN product.updated_at IS 'Дата последнего изменения';
COMMENT ON COLUMN product.version IS 'Версия сущности';

--rollback DROP TABLE product;
