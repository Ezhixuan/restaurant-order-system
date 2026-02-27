-- ============================================
-- 修复数据库中文乱码问题
-- 将数据库和所有表统一设置为 utf8mb4
-- ============================================

USE restaurant_order;

-- 1. 设置数据库字符集
ALTER DATABASE restaurant_order
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 2. 修复 dish 表
ALTER TABLE dish CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 修复 dish_category 表
ALTER TABLE dish_category CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. 修复 dish_spec 表
ALTER TABLE dish_spec CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 5. 修复 dish_recipe 表
ALTER TABLE dish_recipe CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 6. 修复 inventory_item 表
ALTER TABLE inventory_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 7. 修复 inventory_transaction 表
ALTER TABLE inventory_transaction CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 8. 修复 orders 表
ALTER TABLE orders CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 9. 修复 order_item 表
ALTER TABLE order_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 10. 修复 payment_setting 表
ALTER TABLE payment_setting CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 11. 修复 restaurant_table 表
ALTER TABLE restaurant_table CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 12. 修复 sys_user 表
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 验证修复结果
SELECT '数据库字符集修复完成' as result;
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'restaurant_order';
