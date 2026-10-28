CREATE TABLE word_card
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    word        VARCHAR(200) NOT NULL,
    translation VARCHAR(500) NOT NULL,
    language    VARCHAR(8)   NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_word_card_user_word UNIQUE (user_id, language, word)
);

CREATE INDEX ix_word_card_user_created ON word_card (user_id, created_at DESC);
