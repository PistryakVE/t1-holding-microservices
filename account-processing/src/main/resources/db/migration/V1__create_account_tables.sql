CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(12) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    interest_rate DECIMAL(5,2),
    is_recalc BOOLEAN DEFAULT FALSE,
    card_exist BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    card_id VARCHAR(20) NOT NULL UNIQUE,
    payment_system VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    is_credit BOOLEAN NOT NULL,
    payed_at TIMESTAMP,
    type VARCHAR(20) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    card_id BIGINT,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (card_id) REFERENCES cards(id)
);
