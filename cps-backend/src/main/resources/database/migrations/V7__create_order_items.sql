CREATE TABLE order_items
(
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id       UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id     UUID           NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity       INTEGER        NOT NULL CHECK (quantity > 0),
    purchase_price NUMERIC(10, 2) NOT NULL CHECK (purchase_price > 0)
);
