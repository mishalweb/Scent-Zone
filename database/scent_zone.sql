-- Scent Zone schema: execute this file once in MySQL Workbench.
-- Sample catalog prices are illustrative BDT values.
CREATE DATABASE IF NOT EXISTS scent_zone_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE scent_zone_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS brands (
    brand_id INT AUTO_INCREMENT PRIMARY KEY,
    brand_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS perfumes (
    perfume_id INT AUTO_INCREMENT PRIMARY KEY,
    brand_id INT NOT NULL,
    perfume_name VARCHAR(150) NOT NULL,
    category VARCHAR(60) NOT NULL,
    size_ml INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_perfume_brand FOREIGN KEY (brand_id) REFERENCES brands(brand_id),
    CONSTRAINT chk_perfume_size CHECK (size_ml > 0),
    CONSTRAINT chk_perfume_price CHECK (price >= 0),
    CONSTRAINT chk_perfume_stock CHECK (stock_quantity >= 0)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')
        NOT NULL DEFAULT 'PENDING',
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_order_total CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    perfume_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_orderitem_perfume FOREIGN KEY (perfume_id) REFERENCES perfumes(perfume_id),
    CONSTRAINT chk_orderitem_quantity CHECK (quantity > 0),
    CONSTRAINT chk_orderitem_price CHECK (unit_price >= 0)
);

INSERT IGNORE INTO brands (brand_id, brand_name) VALUES
    (1, 'Rasasi'),
    (2, 'French Avenue'),
    (3, 'Afnan'),
    (4, 'Lattafa'),
    (5, 'Rayhaan');

INSERT IGNORE INTO perfumes
    (perfume_id, brand_id, perfume_name, category, size_ml, price, stock_quantity, description)
VALUES
    (1, 1, 'Hawas Ice', 'Fresh / Aquatic', 100, 5500.00, 12,
     'Crisp fresh fragrance with an aquatic character.'),
    (2, 1, 'Hawas Black', 'Woody / Aromatic', 100, 4800.00, 8,
     'Bold woody fragrance with aromatic notes.'),
    (3, 2, 'Liquid Brun', 'Warm / Gourmand', 100, 4500.00, 10,
     'Warm and cozy gourmand-style scent.'),
    (4, 3, '9PM', 'Sweet / Evening', 100, 3800.00, 15,
     'Sweet scent designed for evenings.'),
    (5, 4, 'Asad', 'Spicy / Amber', 100, 2500.00, 20,
     'Spicy, amber-forward fragrance.'),
    (6, 5, 'Lion', 'Fresh / Woody', 100, 3300.00, 7,
     'Fresh, woody scent for everyday wear.');

-- No default admin password is included. Create the first admin using AdminSetup.
