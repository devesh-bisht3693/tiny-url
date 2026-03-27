CREATE TABLE short_urls (
    id          BIGINT PRIMARY KEY,
    slug        VARCHAR(32) NOT NULL,
    long_url    TEXT        NOT NULL,
    url_hash    BYTEA       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    click_count BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_short_urls_slug UNIQUE (slug),
    CONSTRAINT uq_short_urls_url_hash UNIQUE (url_hash)
);

CREATE INDEX idx_short_urls_created_at ON short_urls (created_at);
