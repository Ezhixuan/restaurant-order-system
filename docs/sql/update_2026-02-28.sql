-- ============================================
-- 数据库更新脚本
-- 更新日期: 2026-02-28
-- 说明: 补全缺失的表结构和模拟数据
-- ============================================

USE restaurant_order;

-- ============================================
-- 1. 添加 dish_spec 菜品规格表
-- ============================================
CREATE TABLE IF NOT EXISTS `dish_spec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `name` VARCHAR(50) NOT NULL COMMENT '规格名称: 小份/中份/大份/例份等',
  `price` DECIMAL(10,2) NOT NULL COMMENT '规格价格',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_dish_id` (`dish_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格表';

-- ============================================
-- 2. 更新 order_item 表，添加缺失字段 (MySQL 8.0)
-- ============================================
-- 检查并添加 spec_id 字段
SET @exists_spec_id := (SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = 'restaurant_order'
                        AND table_name = 'order_item'
                        AND column_name = 'spec_id');
SET @sql_spec_id := IF(@exists_spec_id = 0,
                       'ALTER TABLE `order_item` ADD COLUMN `spec_id` BIGINT DEFAULT NULL COMMENT "规格ID(可选)" AFTER `dish_id`',
                       'SELECT "spec_id already exists"');
PREPARE stmt_spec_id FROM @sql_spec_id;
EXECUTE stmt_spec_id;
DEALLOCATE PREPARE stmt_spec_id;

-- 检查并添加 spec_name 字段
SET @exists_spec_name := (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = 'restaurant_order'
                          AND table_name = 'order_item'
                          AND column_name = 'spec_name');
SET @sql_spec_name := IF(@exists_spec_name = 0,
                         'ALTER TABLE `order_item` ADD COLUMN `spec_name` VARCHAR(50) DEFAULT NULL COMMENT "规格名称(快照)" AFTER `dish_name`',
                         'SELECT "spec_name already exists"');
PREPARE stmt_spec_name FROM @sql_spec_name;
EXECUTE stmt_spec_name;
DEALLOCATE PREPARE stmt_spec_name;

-- 检查并添加 is_paid 字段
SET @exists_is_paid := (SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = 'restaurant_order'
                        AND table_name = 'order_item'
                        AND column_name = 'is_paid');
SET @sql_is_paid := IF(@exists_is_paid = 0,
                       'ALTER TABLE `order_item` ADD COLUMN `is_paid` TINYINT DEFAULT 0 COMMENT "是否已结账: 0否 1是" AFTER `status`',
                       'SELECT "is_paid already exists"');
PREPARE stmt_is_paid FROM @sql_is_paid;
EXECUTE stmt_is_paid;
DEALLOCATE PREPARE stmt_is_paid;

-- ============================================
-- 3. 原料库存管理系统表
-- ============================================

-- 原料表
CREATE TABLE IF NOT EXISTS `inventory_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '原料ID',
  `code` VARCHAR(50) DEFAULT NULL COMMENT '原料编码',
  `name` VARCHAR(100) NOT NULL COMMENT '原料名称',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类：肉类/蔬菜/调料等',
  `main_unit` VARCHAR(20) NOT NULL DEFAULT 'g' COMMENT '主单位：g(克)',
  `aux_unit` VARCHAR(20) DEFAULT NULL COMMENT '辅助单位：斤/公斤/份',
  `conversion_rate` DECIMAL(10,4) DEFAULT 1.0000 COMMENT '换算系数',
  `current_stock` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当前库存（主单位）',
  `reserved_stock` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已预占库存',
  `available_stock` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '可用库存',
  `safety_stock` DECIMAL(10,2) DEFAULT 0 COMMENT '安全库存阈值',
  `warning_status` TINYINT DEFAULT 0 COMMENT '预警状态：0正常 1低于安全库存 2缺货',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  INDEX `idx_category` (`category`),
  INDEX `idx_status` (`status`),
  INDEX `idx_warning` (`warning_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原料表';

-- 菜品配方表
CREATE TABLE IF NOT EXISTS `dish_recipe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `spec_id` BIGINT DEFAULT NULL COMMENT '规格ID（null表示基础配方）',
  `inventory_item_id` BIGINT NOT NULL COMMENT '原料ID',
  `quantity` DECIMAL(10,2) NOT NULL COMMENT '用量（主单位）',
  `is_main` TINYINT DEFAULT 1 COMMENT '是否主料：0辅料 1主料',
  `sort_order` INT DEFAULT 0,
  `is_deleted` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_inventory` (`dish_id`, `spec_id`, `inventory_item_id`),
  INDEX `idx_dish_id` (`dish_id`),
  INDEX `idx_inventory_item_id` (`inventory_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品配方表';

-- 库存事务记录表
CREATE TABLE IF NOT EXISTS `inventory_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inventory_item_id` BIGINT NOT NULL COMMENT '原料ID',
  `transaction_type` TINYINT NOT NULL COMMENT '类型：1入库 2出库 3预占 4释放 5盘点调整',
  `quantity` DECIMAL(10,2) NOT NULL COMMENT '数量（正数增加，负数减少）',
  `before_stock` DECIMAL(10,2) NOT NULL COMMENT '操作前库存',
  `after_stock` DECIMAL(10,2) NOT NULL COMMENT '操作后库存',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `order_item_id` BIGINT DEFAULT NULL COMMENT '关联订单项ID',
  `remark` VARCHAR(500) DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_inventory_item_id` (`inventory_item_id`),
  INDEX `idx_transaction_type` (`transaction_type`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事务记录表';

-- ============================================
-- 4. 插入模拟数据
-- ============================================

-- 菜品规格数据
INSERT INTO `dish_spec` (`dish_id`, `name`, `price`, `sort_order`, `status`) VALUES
(1, '小份', 28.00, 1, 1),  -- 宫保鸡丁小份
(1, '大份', 48.00, 2, 1),  -- 宫保鸡丁大份
(2, '小份', 48.00, 1, 1),  -- 水煮鱼小份
(2, '大份', 88.00, 2, 1),  -- 水煮鱼大份
(3, '例份', 18.00, 1, 1),  -- 麻婆豆腐
(4, '例份', 28.00, 1, 1),  -- 回锅肉
(5, '小份', 38.00, 1, 1),  -- 糖醋排骨小份
(5, '大份', 58.00, 2, 1),  -- 糖醋排骨大份
(10, '小份', 12.00, 1, 1), -- 蛋炒饭小份
(10, '大份', 20.00, 2, 1); -- 蛋炒饭大份

-- 原料数据
INSERT INTO `inventory_item` (`code`, `name`, `category`, `main_unit`, `aux_unit`, `conversion_rate`, `current_stock`, `safety_stock`, `warning_status`, `status`) VALUES
('MEAT001', '鸡胸肉', '肉类', 'g', 'kg', 1000.0000, 12500.00, 5000.00, 0, 1),
('MEAT002', '猪肉', '肉类', 'g', 'kg', 1000.0000, 8200.00, 3000.00, 0, 1),
('MEAT003', '牛肉', '肉类', 'g', 'kg', 1000.0000, 5600.00, 2000.00, 0, 1),
('MEAT004', '鱼肉', '肉类', 'g', 'kg', 1000.0000, 4300.00, 2000.00, 0, 1),
('MEAT005', '虾仁', '肉类', 'g', 'kg', 1000.0000, 2800.00, 1500.00, 1, 1),
('VEG001', '青菜', '蔬菜', 'g', 'kg', 1000.0000, 0.00, 2000.00, 2, 1),
('VEG002', '土豆', '蔬菜', 'g', 'kg', 1000.0000, 3500.00, 5000.00, 1, 1),
('VEG003', '西红柿', '蔬菜', 'g', 'kg', 1000.0000, 6200.00, 3000.00, 0, 1),
('VEG004', '黄瓜', '蔬菜', 'g', 'kg', 1000.0000, 4100.00, 2000.00, 0, 1),
('VEG005', '木耳', '蔬菜', 'g', 'kg', 1000.0000, 1500.00, 1000.00, 0, 1),
('VEG006', '豆腐', '豆制品', 'g', 'kg', 1000.0000, 3200.00, 2000.00, 0, 1),
('SPICE001', '干辣椒', '调料', 'g', 'kg', 1000.0000, 2300.00, 1000.00, 0, 1),
('SPICE002', '花椒', '调料', 'g', 'kg', 1000.0000, 850.00, 500.00, 0, 1),
('SPICE003', '大蒜', '调料', 'g', 'kg', 1000.0000, 5000.00, 2000.00, 0, 1),
('SPICE004', '生姜', '调料', 'g', 'kg', 1000.0000, 1800.00, 1000.00, 0, 1),
('GRAIN001', '大米', '主食', 'g', 'kg', 1000.0000, 25000.00, 10000.00, 0, 1),
('GRAIN002', '鸡蛋', '蛋禽', '个', NULL, 1.0000, 120.00, 50.00, 0, 1),
('OTHER001', '食用油', '油料', 'ml', 'L', 1000.0000, 15000.00, 5000.00, 0, 1),
('OTHER002', '花生米', '干货', 'g', 'kg', 1000.0000, 3200.00, 1500.00, 0, 1);

-- 菜品配方数据
INSERT INTO `dish_recipe` (`dish_id`, `spec_id`, `inventory_item_id`, `quantity`, `is_main`, `sort_order`) VALUES
-- 宫保鸡丁 (ID=1) 小份配方
(1, 1, 1, 300.00, 1, 1),    -- 鸡胸肉300g
(1, 1, 18, 50.00, 0, 2),    -- 花生米50g
(1, 1, 13, 20.00, 0, 3),    -- 大蒜20g
-- 宫保鸡丁 (ID=1) 大份配方
(1, 2, 1, 500.00, 1, 1),    -- 鸡胸肉500g
(1, 2, 18, 80.00, 0, 2),    -- 花生米80g
(1, 2, 13, 30.00, 0, 3),    -- 大蒜30g
-- 水煮鱼 (ID=2) 小份配方
(2, 3, 4, 400.00, 1, 1),    -- 鱼肉400g
(2, 3, 11, 100.00, 0, 2),   -- 干辣椒100g
(2, 3, 12, 30.00, 0, 3),    -- 花椒30g
-- 水煮鱼 (ID=2) 大份配方
(2, 4, 4, 700.00, 1, 1),    -- 鱼肉700g
(2, 4, 11, 150.00, 0, 2),   -- 干辣椒150g
(2, 4, 12, 50.00, 0, 3),    -- 花椒50g
-- 麻婆豆腐 (ID=3)
(3, NULL, 6, 400.00, 1, 1),  -- 豆腐400g
(3, NULL, 2, 100.00, 1, 2),  -- 猪肉末100g
(3, NULL, 11, 30.00, 0, 3),  -- 干辣椒30g
-- 回锅肉 (ID=4)
(4, NULL, 2, 300.00, 1, 1),  -- 猪肉300g
(4, NULL, 11, 20.00, 0, 2),  -- 干辣椒20g
-- 糖醋排骨 (ID=5) 小份
(5, 5, 2, 300.00, 1, 1),     -- 猪肉300g
-- 糖醋排骨 (ID=5) 大份
(5, 6, 2, 500.00, 1, 1),     -- 猪肉500g
-- 拍黄瓜 (ID=6)
(6, NULL, 4, 300.00, 1, 1),  -- 黄瓜300g
(6, NULL, 13, 15.00, 0, 2),  -- 大蒜15g
-- 凉拌木耳 (ID=7)
(7, NULL, 5, 100.00, 1, 1),  -- 木耳100g
(7, NULL, 13, 10.00, 0, 2),  -- 大蒜10g
-- 酸辣汤 (ID=8)
(8, NULL, 5, 50.00, 1, 1),   -- 木耳50g
(8, NULL, 3, 100.00, 1, 2),  -- 西红柿100g
(8, NULL, 17, 5.00, 0, 3),   -- 鸡蛋1个
-- 紫菜蛋花汤 (ID=9)
(9, NULL, 17, 2.00, 1, 1),   -- 鸡蛋2个
-- 蛋炒饭 (ID=10) 小份
(10, 7, 16, 200.00, 1, 1),   -- 大米200g
(10, 7, 17, 1.00, 1, 2),     -- 鸡蛋1个
-- 蛋炒饭 (ID=10) 大份
(10, 8, 16, 300.00, 1, 1),   -- 大米300g
(10, 8, 17, 2.00, 1, 2);     -- 鸡蛋2个

-- 更多菜品数据
INSERT INTO `dish` (`category_id`, `name`, `description`, `price`, `is_recommend`, `status`, `sort_order`) VALUES
(1, '鱼香肉丝', '酸甜微辣，肉丝滑嫩', 32.00, 1, 1, 4),
(1, '干煸四季豆', '干香麻辣，下饭神器', 26.00, 0, 1, 5),
(2, '蒜蓉西兰花', '蒜香浓郁，营养健康', 22.00, 0, 1, 4),
(2, '地三鲜', '东北名菜，鲜香美味', 24.00, 0, 1, 5),
(2, '西红柿炒蛋', '家常菜之王，酸甜可口', 18.00, 0, 1, 6),
(3, '蒜泥白肉', '肉质鲜嫩，蒜香四溢', 36.00, 0, 1, 3),
(3, '凉拌海带丝', '爽口开胃，低脂健康', 12.00, 0, 1, 4),
(4, '冬瓜排骨汤', '清淡滋补，老少皆宜', 28.00, 0, 1, 3),
(4, '玉米排骨汤', '甜香浓郁，营养丰富', 32.00, 0, 1, 4),
(5, '面条', '劲道爽滑，配汤极佳', 8.00, 0, 1, 3),
(5, '馒头', '松软可口，家常主食', 2.00, 0, 1, 4),
(6, '鲜榨橙汁', '现榨新鲜，维C满满', 15.00, 0, 1, 3),
(6, '豆浆', '现磨香浓，营养早餐', 5.00, 0, 1, 4);

-- 更新 available_stock 计算值
UPDATE `inventory_item` SET `available_stock` = `current_stock` - `reserved_stock`;

-- 插入支付设置
INSERT INTO `payment_setting` (`type`, `name`, `qrcode_image`, `is_default`, `status`) VALUES
(1, '微信支付', 'https://example.com/wechat-pay.png', 1, 1),
(2, '支付宝', 'https://example.com/alipay.png', 0, 1);

-- 插入一些示例订单数据
INSERT INTO `orders` (`order_no`, `table_id`, `table_no`, `customer_count`, `total_amount`, `discount_amount`, `pay_amount`, `pay_type`, `pay_time`, `status`, `remark`) VALUES
('ORD202602280001', 1, 'A01', 6, 268.00, 0.00, 268.00, 3, NOW() - INTERVAL 2 HOUR, 4, '包厢客人，要求加辣'),
('ORD202602280002', 3, 'B01', 4, 156.00, 0.00, 156.00, 1, NOW() - INTERVAL 1 HOUR, 3, ''),
('ORD202602280003', 5, 'B03', 2, 89.00, 0.00, 89.00, 2, NOW() - INTERVAL 30 MINUTE, 2, ''),
('ORD202602280004', 2, 'A02', 4, 198.00, 0.00, 198.00, 0, NULL, 1, '暂不上菜，等人到齐'),
('ORD202602280005', 4, 'B02', 3, 125.00, 0.00, 125.00, 0, NULL, 0, '');

-- 插入订单明细
INSERT INTO `order_item` (`order_id`, `dish_id`, `spec_id`, `dish_name`, `spec_name`, `price`, `quantity`, `subtotal`, `status`, `is_paid`) VALUES
-- 订单1
(1, 1, 2, '宫保鸡丁', '大份', 48.00, 1, 48.00, 2, 1),
(1, 2, 4, '水煮鱼', '大份', 88.00, 1, 88.00, 2, 1),
(1, 6, NULL, '拍黄瓜', NULL, 12.00, 1, 12.00, 2, 1),
(1, 10, 8, '蛋炒饭', '大份', 20.00, 3, 60.00, 2, 1),
(1, 13, NULL, '米饭', NULL, 2.00, 6, 12.00, 2, 1),
(1, 11, NULL, '雪碧', NULL, 5.00, 4, 20.00, 2, 1),
-- 订单2
(2, 4, NULL, '回锅肉', NULL, 28.00, 1, 28.00, 2, 1),
(2, 3, NULL, '麻婆豆腐', NULL, 18.00, 1, 18.00, 2, 1),
(2, 16, NULL, '西红柿炒蛋', NULL, 18.00, 1, 18.00, 2, 1),
(2, 6, NULL, '拍黄瓜', NULL, 12.00, 1, 12.00, 2, 1),
(2, 13, NULL, '米饭', NULL, 2.00, 4, 8.00, 2, 1),
(2, 12, NULL, '可乐', NULL, 5.00, 2, 10.00, 2, 1),
(2, 24, NULL, '玉米排骨汤', NULL, 32.00, 1, 32.00, 2, 1),
(2, 22, NULL, '冬瓜排骨汤', NULL, 28.00, 1, 28.00, 2, 1),
-- 订单3
(3, 1, 1, '宫保鸡丁', '小份', 28.00, 1, 28.00, 2, 1),
(3, 15, NULL, '蒜蓉西兰花', NULL, 22.00, 1, 22.00, 2, 1),
(3, 13, NULL, '米饭', NULL, 2.00, 2, 4.00, 2, 1),
(3, 26, NULL, '豆浆', NULL, 5.00, 2, 10.00, 2, 1),
(3, 25, NULL, '鲜榨橙汁', NULL, 15.00, 1, 15.00, 2, 1),
-- 订单4
(4, 2, 3, '水煮鱼', '小份', 48.00, 1, 48.00, 0, 0),
(4, 14, NULL, '蒜泥白肉', NULL, 36.00, 1, 36.00, 0, 0),
(4, 17, NULL, '地三鲜', NULL, 24.00, 1, 24.00, 0, 0),
(4, 8, NULL, '酸辣汤', NULL, 12.00, 1, 12.00, 0, 0),
(4, 13, NULL, '米饭', NULL, 2.00, 4, 8.00, 0, 0),
(4, 11, NULL, '雪碧', NULL, 5.00, 2, 10.00, 0, 0),
-- 订单5
(5, 5, 5, '糖醋排骨', '小份', 38.00, 1, 38.00, 1, 0),
(5, 7, NULL, '凉拌木耳', NULL, 16.00, 1, 16.00, 1, 0),
(5, 9, NULL, '紫菜蛋花汤', NULL, 10.00, 1, 10.00, 1, 0),
(5, 10, 7, '蛋炒饭', '小份', 12.00, 2, 24.00, 1, 0),
(5, 12, NULL, '可乐', NULL, 5.00, 2, 10.00, 1, 0),
(5, 27, NULL, '馒头', NULL, 2.00, 4, 8.00, 1, 0);
