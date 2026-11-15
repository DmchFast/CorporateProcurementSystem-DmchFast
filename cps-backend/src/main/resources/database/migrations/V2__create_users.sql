CREATE TYPE user_role AS ENUM ('USER', 'MANAGER');

CREATE TABLE users
(
    id         UUID PRIMARY KEY             DEFAULT uuidv7(),
    username   VARCHAR(255) UNIQUE NOT NULL,
    user_role  user_role           NOT NULL,
    created_at TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);
