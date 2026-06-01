CREATE TABLE IF NOT EXISTS PRODUCTS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    NAME VARCHAR(255) NOT NULL,
    DESCRIPTION VARCHAR(1000),
    PRICE DECIMAL(10, 2) NOT NULL,
    STOCK INT NOT NULL DEFAULT 0
);

INSERT INTO PRODUCTS (NAME, DESCRIPTION, PRICE, STOCK) VALUES
('Laptop', 'High-performance laptop with 16GB RAM', 999.99, 50),
('Headphones', 'Wireless noise-cancelling headphones', 299.99, 100),
('Keyboard', 'Mechanical gaming keyboard', 149.99, 75);
