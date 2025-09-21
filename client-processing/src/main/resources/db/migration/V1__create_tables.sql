CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    product_key VARCHAR(10) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    product_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(12) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(255),
    middle_name VARCHAR(255),
    last_name VARCHAR(255),
    date_of_birth DATE,
    document_type VARCHAR(20),
    document_id VARCHAR(255),
    document_prefix VARCHAR(10),
    document_suffix VARCHAR(10),
    FOREIGN KEY (user_id) REFERENCES users(id)
);


CREATE TABLE client_products (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    open_date DATE NOT NULL,
    close_date DATE,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
