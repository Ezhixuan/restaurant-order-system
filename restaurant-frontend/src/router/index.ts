import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/admin/login'
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/admin/Dashboard.vue')
  },
  {
    path: '/admin/dishes',
    name: 'AdminDishes',
    component: () => import('@/views/admin/Dishes.vue')
  },
  {
    path: '/admin/tables',
    name: 'AdminTables',
    component: () => import('@/views/admin/Tables.vue')
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('@/views/admin/Orders.vue')
  },
  {
    path: '/admin/reports',
    name: 'AdminReports',
    component: () => import('@/views/admin/Reports.vue')
  },
  // Pad routes
  {
    path: '/pad/login',
    name: 'PadLogin',
    component: () => import('@/views/pad/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/pad',
    redirect: '/pad/tables'
  },
  {
    path: '/pad/tables',
    name: 'PadTables',
    component: () => import('@/views/pad/Tables.vue')
  },
  {
    path: '/pad/order',
    name: 'PadOrder',
    component: () => import('@/views/pad/OrderPad.vue')
  },
  // Mobile routes
  {
    path: '/m/:tableNo',
    name: 'MobileMenu',
    component: () => import('@/views/mobile/Menu.vue'),
    meta: { public: true }
  },
  {
    path: '/m/cart',
    name: 'MobileCart',
    component: () => import('@/views/mobile/Cart.vue'),
    meta: { public: true }
  },
  {
    path: '/m/order',
    name: 'MobileOrder',
    component: () => import('@/views/mobile/Order.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.public) {
    next()
    return
  }
  
  if (!token && to.path.startsWith('/admin')) {
    next('/admin/login')
    return
  }
  
  if (!token && to.path.startsWith('/pad')) {
    next('/pad/login')
    return
  }
  
  next()
})

export default router
