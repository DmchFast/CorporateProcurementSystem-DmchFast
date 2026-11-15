CREATE TABLE audit_logs
(
    id         UUID PRIMARY KEY      DEFAULT uuidv7(),
    order_id   UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    changed_by UUID         NOT NULL REFERENCES users (id),
    old_status order_status NOT NULL,
    new_status order_status NOT NULL,
    changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
