-- ============================================
-- 小型餐馆点餐系统 - 数据库初始化脚本
-- 数据库: restaurant_order
-- 创建时间: 2026-02-25
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS restaurant_order 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE restaurant_order;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` TINYINT DEFAULT 1 COMMENT '角色: 1服务员 2管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_phone` (`phone`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 桌台表 (固定卡座 + 临时座位)
-- ============================================
CREATE TABLE IF NOT EXISTS `restaurant_table` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '桌台ID',
  `table_no` VARCHAR(20) NOT NULL COMMENT '桌号: A01、临1等',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '桌台名称: 包厢1、路边1号桌',
  `type` TINYINT DEFAULT 1 COMMENT '类型: 1固定卡座 2临时座位',
  `capacity` INT DEFAULT 4 COMMENT '容纳人数',
  `qrcode` VARCHAR(255) DEFAULT NULL COMMENT '点餐二维码(固定座位才有)',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0空闲 1使用中 2待清台',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_no` (`table_no`),
  INDEX `idx_type` (`type`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌台表';

-- ============================================
-- 3. 菜品分类表
-- ============================================
CREATE TABLE IF NOT EXISTS `dish_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- ============================================
-- 4. 菜品表
-- ============================================
CREATE TABLE IF NOT EXISTS `dish` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
  `description` TEXT COMMENT '菜品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '菜品图片',
  `stock` INT DEFAULT 999 COMMENT '库存(-1表示不限)',
  `is_recommend` TINYINT DEFAULT 0 COMMENT '是否推荐: 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0下架 1上架',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `has_specs` TINYINT DEFAULT 0 COMMENT '是否有规格: 0无 1有',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_category_id` (`category_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_recommend` (`is_recommend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- ============================================
-- 5. 订单表
-- ============================================
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `table_id` BIGINT COMMENT '桌台ID',
  `table_no` VARCHAR(20) COMMENT '桌号',
  `customer_count` INT DEFAULT 1 COMMENT '用餐人数',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_type` TINYINT DEFAULT 0 COMMENT '支付方式: 0未支付 1微信 2支付宝 3现金',
  `pay_time` DATETIME COMMENT '支付时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待支付 1已支付 2制作中 3待上菜 4已完成 5已取消',
  `remark` VARCHAR(500) COMMENT '订单备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  INDEX `idx_table_id` (`table_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 6. 订单明细表
-- ============================================
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `dish_name` VARCHAR(100) NOT NULL COMMENT '菜品名称(快照)',
  `dish_image` VARCHAR(255) COMMENT '菜品图片(快照)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
  `quantity` INT NOT NULL COMMENT '数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `remark` VARCHAR(200) COMMENT '备注',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待制作 1制作中 2已完成',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================
-- 7. 支付设置表 (收款码)
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `type` TINYINT NOT NULL COMMENT '类型: 1微信 2支付宝',
  `name` VARCHAR(50) COMMENT '名称',
  `qrcode_image` VARCHAR(255) COMMENT '收款码图片URL',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付设置表';

-- ============================================
-- 初始化数据
-- ============================================

-- 默认管理员账号 (密码: admin123)
-- 使用BCrypt加密: $2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW', '管理员', 2, 1);

-- 服务员账号 (密码: 123456)
-- 使用BCrypt加密: $2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('waiter', '$2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW', '服务员1', 1, 1);

-- 示例固定桌台 (8个卡座 + 2个包厢)
INSERT INTO `restaurant_table` (`table_no`, `name`, `type`, `capacity`, `status`, `sort_order`) VALUES
('A01', '包厢1', 1, 8, 0, 1),
('A02', '包厢2', 1, 6, 0, 2),
('B01', '卡座1', 1, 4, 0, 3),
('B02', '卡座2', 1, 4, 0, 4),
('B03', '卡座3', 1, 4, 0, 5),
('B04', '卡座4', 1, 4, 0, 6),
('B05', '卡座5', 1, 4, 0, 7),
('B06', '卡座6', 1, 4, 0, 8),
('B07', '卡座7', 1, 4, 0, 9),
('B08', '卡座8', 1, 4, 0, 10);

-- 示例菜品分类
INSERT INTO `dish_category` (`name`, `sort_order`, `status`) VALUES
('招牌菜', 1, 1),
('热菜', 2, 1),
('凉菜', 3, 1),
('汤羹', 4, 1),
('主食', 5, 1),
('饮品', 6, 1);

-- 示例菜品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price`, `is_recommend`, `status`, `sort_order`) VALUES
(1, '宫保鸡丁', '经典川菜，鸡肉鲜嫩，花生香脆', 38.00, 1, 1, 1),
(1, '水煮鱼', '麻辣鲜香，鱼肉嫩滑', 68.00, 1, 1, 2),
(2, '麻婆豆腐', '麻辣鲜香，下饭神器', 18.00, 0, 1, 1),
(2, '回锅肉', '肥而不腻，香味浓郁', 28.00, 0, 1, 2),
(2, '糖醋排骨', '酸甜可口，老少皆宜', 48.00, 1, 1, 3),
(3, '拍黄瓜', '清爽解腻，开胃小菜', 12.00, 0, 1, 1),
(3, '凉拌木耳', '营养丰富，爽口健康', 16.00, 0, 1, 2),
(4, '酸辣汤', '酸辣开胃，暖胃佳品', 12.00, 0, 1, 1),
(4, '紫菜蛋花汤', '清淡鲜美，老少皆宜', 10.00, 0, 1, 2),
(5, '米饭', '香喷喷的大米饭', 2.00, 0, 1, 1),
(5, '蛋炒饭', '金黄诱人，粒粒分明', 15.00, 0, 1, 2),
(6, '雪碧', '清爽碳酸饮料', 5.00, 0, 1, 1),
(6, '可乐', '经典可口可乐', 5.00, 0, 1, 2);
