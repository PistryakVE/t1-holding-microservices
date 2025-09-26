ALTER TABLE product_registry ADD COLUMN month_count INT;

-- Обновить существующие записи
UPDATE product_registry SET month_count = 12 WHERE month_count IS NULL;