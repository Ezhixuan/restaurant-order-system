# Restaurant Order System - Frontend

Vue 3 + Vite + TypeScript

## 技术栈

- Vue 3 (Composition API)
- Vite 5.x
- TypeScript 5.x
- Element Plus (Web/Pad)
- Vant 4.x (Mobile)
- Pinia (State Management)
- Vue Router 4
- Axios

## 项目结构

```
restaurant-frontend/
├── src/
│   ├── api/              # API接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia状态
│   ├── utils/            # 工具函数
│   └── views/            # 页面
│       ├── mobile/       # 顾客端
│       ├── pad/          # 服务员端
│       └── admin/        # 管理后台
└── package.json
```

## 运行

```bash
# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建
npm run build
```
