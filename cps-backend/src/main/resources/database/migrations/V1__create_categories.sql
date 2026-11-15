CREATE TABLE categories
(
    id   UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL
);
