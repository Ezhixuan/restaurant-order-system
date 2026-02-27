-- ============================================
-- 餐厅点餐系统 - 完整数据库初始化脚本
-- 数据库: restaurant_order
-- 字符集: utf8mb4 (支持完整中文和emoji)
-- 创建时间: 2026-02-28
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除已存在的数据库（谨慎使用）
DROP DATABASE IF EXISTS restaurant_order;

-- 创建数据库，使用 utf8mb4 字符集
CREATE DATABASE restaurant_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE restaurant_order;

-- 设置会话字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- ============================================
-- 1. 用户表 (sys_user)
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
  KEY `idx_phone` (`phone`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 桌台表 (restaurant_table)
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
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌台表';

-- ============================================
-- 3. 菜品分类表 (dish_category)
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
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类表';

-- ============================================
-- 4. 菜品表 (dish)
-- ============================================
CREATE TABLE IF NOT EXISTS `dish` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
  `description` TEXT COMMENT '菜品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价(无规格时的基础价格)',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '菜品图片',
  `stock` INT DEFAULT 999 COMMENT '库存(-1表示不限)',
  `is_recommend` TINYINT DEFAULT 0 COMMENT '是否推荐: 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0下架 1上架',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `has_specs` TINYINT DEFAULT 0 COMMENT '是否有规格: 0无 1有',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_recommend` (`is_recommend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

-- ============================================
-- 5. 菜品规格表 (dish_spec)
-- ============================================
CREATE TABLE IF NOT EXISTS `dish_spec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `name` VARCHAR(50) NOT NULL COMMENT '规格名称: 小份/中份/大份/例份等',
  `price` DECIMAL(10,2) NOT NULL COMMENT '规格价格',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品规格表';

-- ============================================
-- 6. 订单表 (orders)
-- ============================================
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `table_id` BIGINT DEFAULT NULL COMMENT '桌台ID',
  `table_no` VARCHAR(20) DEFAULT NULL COMMENT '桌号',
  `customer_count` INT DEFAULT 1 COMMENT '用餐人数',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_type` TINYINT DEFAULT 0 COMMENT '支付方式: 0未支付 1微信 2支付宝 3现金',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待上菜 1上菜中 2待结账 3已完成 4已取消',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 7. 订单明细表 (order_item)
-- ============================================
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `spec_id` BIGINT DEFAULT NULL COMMENT '规格ID(可选)',
  `dish_name` VARCHAR(100) NOT NULL COMMENT '菜品名称(快照)',
  `spec_name` VARCHAR(50) DEFAULT NULL COMMENT '规格名称(快照)',
  `dish_image` VARCHAR(255) DEFAULT NULL COMMENT '菜品图片(快照)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
  `quantity` INT NOT NULL COMMENT '数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0待制作 1制作中 2已完成',
  `is_paid` TINYINT DEFAULT 0 COMMENT '是否已结账: 0否 1是',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- ============================================
-- 8. 支付设置表 (payment_setting)
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `type` TINYINT NOT NULL COMMENT '类型: 1微信 2支付宝',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '名称',
  `qrcode_image` VARCHAR(255) DEFAULT NULL COMMENT '收款码图片URL',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付设置表';

-- ============================================
-- 9. 原料表 (inventory_item) - 库存管理
-- ============================================
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
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_warning` (`warning_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原料表';

-- ============================================
-- 10. 菜品配方表 (dish_recipe)
-- ============================================
CREATE TABLE IF NOT EXISTS `dish_recipe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配方ID',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `spec_id` BIGINT DEFAULT NULL COMMENT '规格ID（null表示基础配方）',
  `inventory_item_id` BIGINT NOT NULL COMMENT '原料ID',
  `quantity` DECIMAL(10,2) NOT NULL COMMENT '用量（主单位）',
  `is_main` TINYINT DEFAULT 1 COMMENT '是否主料：0辅料 1主料',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_inventory` (`dish_id`, `spec_id`, `inventory_item_id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_inventory_item_id` (`inventory_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品配方表';

-- ============================================
-- 11. 库存事务记录表 (inventory_transaction)
-- ============================================
CREATE TABLE IF NOT EXISTS `inventory_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '事务ID',
  `inventory_item_id` BIGINT NOT NULL COMMENT '原料ID',
  `transaction_type` TINYINT NOT NULL COMMENT '类型：1入库 2出库 3预占 4释放 5盘点调整',
  `quantity` DECIMAL(10,2) NOT NULL COMMENT '数量（正数增加，负数减少）',
  `before_stock` DECIMAL(10,2) NOT NULL COMMENT '操作前库存',
  `after_stock` DECIMAL(10,2) NOT NULL COMMENT '操作后库存',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `order_item_id` BIGINT DEFAULT NULL COMMENT '关联订单项ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除: 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_inventory_item_id` (`inventory_item_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存事务记录表';


