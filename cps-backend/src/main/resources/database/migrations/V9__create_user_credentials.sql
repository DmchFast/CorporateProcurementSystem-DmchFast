CREATE TABLE user_credentials
(
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL
);
