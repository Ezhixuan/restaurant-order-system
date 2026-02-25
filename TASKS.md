# 餐厅点餐系统 - 开发任务清单

> 项目路径：/Users/ezhixuan/Documents/code/restaurant-order-system  
> GitHub: https://github.com/Ezhixuan/restaurant-order-system

---

## ✅ 已完成功能

### 后端 (Java/Spring Boot)
- ✅ 用户认证（JWT登录）
- ✅ 桌台管理（固定/临时桌、开台/清台）
- ✅ 菜品管理（分类、CRUD、图片上传）
- ✅ 订单核心（创建、支付、状态流转）
- ✅ 数据统计（营业额、热销菜、桌台统计）
- ✅ WebSocket（后厨实时推送）
- ✅ 文件上传

### 前端 (Vue 3)

#### 管理后台
- ✅ 登录页面
- ✅ Dashboard（数据看板）
- ✅ 桌台管理
- ✅ 菜品管理（含图片上传）
- ✅ 订单管理（列表、详情、状态更新）
- ✅ 后厨页面（WebSocket实时订单）

#### Pad端（服务员）
- ✅ 登录页面
- ✅ 桌台列表（开台/清台）
- ✅ 点餐页面（分类、购物车、下单）
- ✅ 订单列表

#### 移动端（顾客）
- ✅ 菜品浏览
- ✅ 购物车
- ✅ 订单确认

---

## 📱 可访问地址

| 端 | 地址 | 账号 |
|----|------|------|
| 管理后台 | http://localhost:5173/admin/login | admin/admin123 |
| 服务员Pad | http://localhost:5173/pad/login | waiter/123456 |
| 后厨页面 | http://localhost:5173/admin/kitchen | admin/admin123 |
| 顾客点餐 | http://localhost:5173/m/B01 | 无需登录 |

---

## 🚀 启动方式

```bash
# 1. 启动后端
cd restaurant-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 启动前端
cd restaurant-frontend
npm run dev
```

---

## ⏳ 待完善功能

- [ ] 支付设置页面（收款码上传）
- [ ] 订单打印功能
- [ ] 库存预警
- [ ] 数据导出
- [ ] Docker部署

---

## 📝 最近更新

- 2026-02-25: Pad点餐页面、订单管理、后厨页面完成
