--liquibase formatted sql

--changeset schum:003-create-processed-events
--comment: Создание таблицы обработанных событий
CREATE TABLE processed_events (
   event_id UUID PRIMARY KEY,
   event_type VARCHAR(50) NOT NULL CHECK (event_type in ('PRODUCT_CREATED', 'PRODUCT_UPDATED')),
   processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE processed_events IS 'Таблица обработанных событий';
COMMENT ON COLUMN processed_events.event_id IS 'ID события';
COMMENT ON COLUMN processed_events.event_type IS 'Тип события';
COMMENT ON COLUMN processed_events.processed_at IS 'Дата обработки события';

--rollback DROP TABLE processed_events;
