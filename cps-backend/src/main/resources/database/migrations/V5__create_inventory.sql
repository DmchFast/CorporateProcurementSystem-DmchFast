CREATE TABLE inventory
(
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    product_id   UUID    NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warehouse_id UUID    NOT NULL REFERENCES warehouses (id) ON DELETE CASCADE,
    quantity     INTEGER NOT NULL CHECK (quantity > 0)
);
