# 餐厅点餐系统 - 使用说明

## 🔐 登录账号

### 默认账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin | admin123 | 管理员 | 管理后台使用 |
| waiter | 123456 | 服务员 | Pad端点餐使用 |

### 登录地址

- **管理后台**: http://localhost:5173/admin/login
- **服务员Pad**: http://localhost:5173/pad/login
- **顾客点餐**: http://localhost:5173/m/{桌号}
  - 示例: http://localhost:5173/m/B01

---

## 🚀 启动项目

### 1. 启动后端

```bash
cd restaurant-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端运行在: http://localhost:8080

### 2. 启动前端

```bash
cd restaurant-frontend
npm run dev
```

前端运行在: http://localhost:5173

---

## 📱 使用流程

### 管理员流程

1. 访问 http://localhost:5173/admin/login
2. 使用 admin/admin123 登录
3. 进入管理后台：
   - **数据看板**: 查看今日营业额、热销菜品
   - **桌台管理**: 添加/编辑固定桌台
   - **菜品管理**: 管理菜品分类和菜品
   - **订单管理**: 查看所有订单

### 服务员流程

1. 访问 http://localhost:5173/pad/login
2. 使用 waiter/123456 登录
3. **桌台页面**:
   - 点击空闲桌台 → 输入人数 → 开台 → 进入点餐
   - 点击使用中桌台 → 继续点餐/加菜
   - 点击待清台桌台 → 确认清台
4. **添加临时桌**: 点击"添加临时桌"按钮

### 顾客流程

1. 扫描桌台二维码（或直接访问 http://localhost:5173/m/{桌号}）
2. 浏览菜品 → 加入购物车
3. 点击购物车 → 确认订单 → 提交
4. 到店扫码支付（展示收款码）

---

## 🛠 添加新用户

### 方法1: 通过SQL直接添加

```sql
-- 添加服务员账号 (密码: 123456)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('waiter2', '$2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW', '服务员2', 1, 1);
```

### 方法2: 通过API添加

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "123456",
    "realName": "新员工",
    "role": 1
  }'
```

---

## 🔧 密码重置

如果需要重置密码为 `123456`:

```sql
UPDATE restaurant_order.sys_user 
SET password='$2a$10$mciZPeRnfG5ItblWmeBKauVHbCEhWCLna3zEZdbNjabOnpZ8daWqW' 
WHERE username='admin';
```

---

## 📊 数据库

- **地址**: localhost:3307
- **数据库**: restaurant_order
- **用户名**: root
- **密码**: 1q2w3e4r%

---

## 📝 注意事项

1. **首次使用**: 先执行数据库初始化脚本 `docs/sql/init.sql`
2. **后端必须先启动**: 前端依赖后端API
3. **Redis**: 可选，如未启动不影响基本功能
