CREATE TABLE products
(
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    sku         VARCHAR(255)   NOT NULL UNIQUE,
    name        VARCHAR(255)   NOT NULL,
    category_id UUID REFERENCES categories (id) ON DELETE CASCADE,
    price       NUMERIC(10, 2) NOT NULL CHECK (price > 0)
);
