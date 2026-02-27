# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

餐厅点餐系统 - A restaurant order management system with three frontend interfaces:
- **Admin Dashboard** (`/admin/*`): Management interface for administrators (Element Plus)
- **Pad Interface** (`/pad/*`): Ordering interface for waitstaff (Element Plus)
- **Mobile Interface** (`/m/:tableNo`): Customer ordering interface (Vant)

## Tech Stack

**Frontend (restaurant-frontend/)**
- Vue 3 + TypeScript + Vite
- Element Plus (Admin/Pad) + Vant 4.x (Mobile)
- Pinia for state management
- Vue Router 4
- Axios with request/response interceptors
- ECharts for data visualization

**Backend (restaurant-backend/)**
- Java 21 + Spring Boot 3.1.12
- MyBatis-Plus 3.5.7 for ORM
- MySQL 8.0 (port 3307 in dev)
- Redis 7.x (optional)
- JWT authentication
- WebSocket for real-time kitchen notifications
- SpringDoc OpenAPI for API documentation

## Common Commands

**Backend**
```bash
cd restaurant-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # Start dev server on port 8080
mvn clean package                                     # Build JAR
```

**Frontend**
```bash
cd restaurant-frontend
npm run dev       # Start dev server on port 5173
npm run build     # Build for production (runs vue-tsc && vite build)
npm run preview   # Preview production build
```

## Project Structure

**Frontend Architecture**
- `src/views/admin/`: Admin dashboard pages (Dashboard, Dishes, Tables, Orders, Kitchen, Reports)
- `src/views/pad/`: Waitstaff pages (Login, Tables, OrderPad, Orders, OrderDetail)
- `src/views/mobile/`: Customer pages (Menu, Cart, Order, OrderSuccess)
- `src/layouts/`: Layout components (AdminLayout, PadLayout, MobileLayout)
- `src/stores/`: Pinia stores (auth.ts, cart.ts)
- `src/api/`: API client modules organized by domain
- `src/utils/request.ts`: Axios instance with JWT interceptor

**Backend Architecture**
- Domain-driven package structure: `auth/`, `user/`, `dish/`, `order/`, `table/`, `report/`, `websocket/`
- Each domain contains: `entity/`, `mapper/`, `service/`, `controller/`, `dto/`
- `common/`: Shared classes (Result, PageResult, BaseEntity, exception handlers)
- `config/`: Security, JWT filter, WebSocket, MyBatis-Plus config

## Key Configuration

**Database (Dev)**
- MySQL: `localhost:3307/restaurant_order`
- Username: `root`, Password: `1q2w3e4r%`
- Init script: `docs/sql/init.sql`

**Default Login Accounts**
- Admin: `admin` / `admin123`
- Waiter: `waiter` / `123456`

**Frontend Proxy (vite.config.ts)**
- `/api/*` proxies to `http://localhost:8080`

## API Patterns

**Response Format**
All API responses use `Result<T>` wrapper:
```json
{ "code": 200, "message": "success", "data": {} }
```

**Authentication**
- JWT token stored in `localStorage` key `token`
- Bearer token sent in Authorization header
- 401 responses redirect to login page

**WebSocket**
- Endpoint: `/ws/kitchen`
- Used for real-time order notifications to kitchen display
- Message types: `NEW_ORDER`, `ORDER_STATUS`, `ITEM_STATUS`

## Order Status Flow

```
0: 待上菜 (Pending Serve)
1: 上菜中 (Serving)
2: 待结账 (Pending Payment)
3: 已完成 (Completed)
4: 已取消 (Cancelled)
```

## Order Item Status (Kitchen)

```
0: 待制作 (Pending Cooking)
1: 制作中 (Cooking)
2: 已完成 (Completed)
```

## Table Status

```
0: 空闲 (Free)
1: 使用中 (Occupied)
2: 待清台 (Needs Cleaning)
```

## Development Notes

- Backend must be started before frontend (frontend depends on backend API)
- Redis is optional - system works without it
- Kitchen display page uses WebSocket for real-time updates
- Mobile cart data persists in Pinia store during session
- Passwords are BCrypt encrypted (default password hash for '123456' is consistent)
