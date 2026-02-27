// 通用响应类型
export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 用户相关
export interface User {
  id: number
  username: string
  realName?: string
  role: 'ADMIN' | 'WAITER'
  status: number
}

export interface LoginResult {
  token: string
  user: User
}

// 餐桌相关
export interface Table {
  id: number
  tableNo: string
  type: number
  capacity: number
  status: 0 | 1 | 2 // 0:空闲 1:使用中 2:待清台
  sortOrder: number
}

export interface TableCreateRequest {
  tableNo: string
  type?: number
  capacity?: number
}

export interface TableUpdateRequest {
  tableNo?: string
  type?: number
  capacity?: number
}

// 菜品分类
export interface Category {
  id: number
  name: string
  sortOrder: number
  status: number
}

// 菜品规格
export interface SpecItem {
  id?: number
  name: string
  price: number
  sortOrder?: number
  status?: number
}

// 菜品相关
export interface Dish {
  id: number
  categoryId: number
  name: string
  description?: string
  price: number
  image?: string
  stock: number
  isRecommend: number
  status: number
  sortOrder: number
  hasSpecs: number
  specs?: SpecItem[]
}

// 订单项
export interface OrderItem {
  id: number
  dishId: number
  dishName: string
  specId?: number
  specName?: string
  price: number
  quantity: number
  status: number // 0:待制作 1:制作中 2:待上菜 3:已完成
  remark?: string
}

// 订单相关
export interface Order {
  id: number
  orderNo: string
  tableId: number
  tableNo: string
  customerCount: number
  status: number // 0:待支付 1:已支付 2:制作中 3:待上菜 4:已完成 5:已取消
  totalAmount: number
  actualAmount?: number
  discountAmount?: number
  remark?: string
  createTime: string
  payTime?: string
  items?: OrderItem[]
}

export interface OrderCreateRequest {
  tableId: number
  customerCount: number
  items: {
    dishId: number
    specId?: number
    quantity: number
    remark?: string
  }[]
}

export interface AddDishRequest {
  dishId: number
  specId?: number
  quantity: number
  remark?: string
}

export interface BatchAddRequest {
  tableId: number
  items: AddDishRequest[]
}

export interface PayRequest {
  amount: number
  discountAmount?: number
  paymentMethod: 'CASH' | 'WECHAT' | 'ALIPAY' | 'CARD'
  remark?: string
}

// 报表相关
export interface TodayStats {
  todayOrderCount: number
  todayRevenue: number
  activeOrderCount: number
  pendingDishCount: number
}

export interface TopDish {
  dishId: number
  dishName: string
  orderCount: number
  totalQuantity: number
  totalAmount: number
}

export interface TableStat {
  tableId: number
  tableNo: string
  orderCount: number
  totalAmount: number
}

// 购物车（从 store 导出）
export interface CartItem {
  dishId: number
  specId?: number
  name: string
  specName?: string
  price: number
  image?: string
  quantity: number
  remark?: string
}
