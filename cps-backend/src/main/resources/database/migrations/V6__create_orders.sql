CREATE TYPE order_status AS ENUM ('CREATED', 'APPROVED', 'DELIVERED', 'CANCELLED');

CREATE TABLE orders
(
    id           UUID PRIMARY KEY      DEFAULT uuidv7(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_status order_status NOT NULL DEFAULT 'CREATED',
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
