CREATE TABLE product_registry (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(12) NOT NULL,
    account_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    open_date DATE NOT NULL
);

CREATE TABLE payment_registry (
    id BIGSERIAL PRIMARY KEY,
    product_registry_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    interest_rate_amount DECIMAL(15,2) NOT NULL,
    debt_amount DECIMAL(15,2) NOT NULL,
    expired BOOLEAN NOT NULL,
    payment_expiration_date DATE NOT NULL,
    FOREIGN KEY (product_registry_id) REFERENCES product_registry(id)
);
