-- Расширение ставится на всю базу; IF NOT EXISTS — чтобы миграция была идемпотентной
CREATE EXTENSION IF NOT EXISTS vector;

-- Структура совпадает с тем, что ожидает PgVectorStore из Spring AI
CREATE TABLE vector_store
(
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1024)
);

CREATE INDEX ix_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops);
