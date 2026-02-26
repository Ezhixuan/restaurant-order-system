# Pad 端菜品份额管理 - 设计方案

## 1. 需求分析

### 1.1 什么是菜品份额？
菜品份额（规格）允许同一菜品有多种份量选择，例如：
- 宫保鸡丁：小份 ¥28 / 中份 ¥38 / 大份 ¥48
- 米饭：小碗 ¥2 / 大碗 ¥3
- 汤类：例份 ¥28 / 中份 ¥48 / 大份 ¥68

### 1.2 Pad 端功能范围

| 功能模块 | Admin 端 | Pad 端 | 说明 |
|---------|---------|--------|------|
| 菜品分类管理 | ✅ | ✅ | Pad 端可新增/编辑分类 |
| 菜品基础信息管理 | ✅ | ✅ | 名称、描述、图片 |
| **菜品份额配置** | ❌ | ✅ | **Pad 端特有** |
| 菜品上下架 | ✅ | ✅ | 快速操作 |
| 库存管理 | ✅ | ⚠️ | Pad 端简化版 |
| 推荐设置 | ✅ | ❌ | 仅 Admin |
| 价格修改 | ✅ | ✅ | 实时调整 |

---

## 2. 数据模型设计

### 2.1 现有模型

```
Dish (菜品)
├── id
├── categoryId      # 分类ID
├── name            # 菜品名称
├── price           # 价格 ⚠️ 需要扩展为份额价格
├── image
├── stock
└── status
```

### 2.2 新模型设计

**方案：份额扩展表（推荐）**

```sql
-- 菜品表 (保持简洁)
CREATE TABLE dish (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    description TEXT COMMENT '菜品描述',
    image VARCHAR(255) COMMENT '菜品图片',
    stock INTEGER DEFAULT 999 COMMENT '库存(-1表示不限)',
    is_recommend TINYINT DEFAULT 0 COMMENT '是否推荐',
    status TINYINT DEFAULT 1 COMMENT '状态: 0下架 1上架',
    sort_order INTEGER DEFAULT 0 COMMENT '排序',
    -- 新增字段
    has_specs TINYINT DEFAULT 0 COMMENT '是否有规格: 0无 1有',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 菜品份额规格表 (新增)
CREATE TABLE dish_spec (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    name VARCHAR(50) NOT NULL COMMENT '规格名称: 小份/中份/大份',
    price DECIMAL(10,2) NOT NULL COMMENT '规格价格',
    sort_order INTEGER DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_dish_id (dish_id),
    INDEX idx_status (status)
);

-- 订单明细表修改 (支持份额)
ALTER TABLE order_item ADD COLUMN spec_id BIGINT COMMENT '规格ID' AFTER dish_id;
ALTER TABLE order_item ADD COLUMN spec_name VARCHAR(50) COMMENT '规格名称快照' AFTER dish_name;
```

### 2.3 实体类设计

```java
// Dish.java - 修改
@Data
@TableName("dish")
public class Dish extends BaseEntity {
    private Long categoryId;
    private String name;
    private String description;
    private String image;
    private Integer stock;
    private Integer isRecommend;
    private Integer status;
    private Integer sortOrder;
    private Integer hasSpecs;  // 新增: 是否有规格
    
    @TableField(exist = false)
    private List<DishSpec> specs;  // 非持久化字段
    
    @TableField(exist = false)
    private BigDecimal basePrice;  // 无规格时的价格
}

// DishSpec.java - 新增
@Data
@TableName("dish_spec")
public class DishSpec extends BaseEntity {
    private Long dishId;
    private String name;        // 规格名称: 小份/中份/大份
    private BigDecimal price;   // 规格价格
    private Integer sortOrder;
    private Integer status;     // 0禁用 1启用
}

// OrderItem.java - 修改
@Data
@TableName("order_item")
public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long dishId;
    private Long specId;           // 新增: 规格ID
    private String dishName;
    private String specName;       // 新增: 规格名称快照
    private String dishImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String remark;
    private Integer status;
    private Integer isPaid;
}
```

---

## 3. API 设计

### 3.1 份额管理接口

```java
@RestController
@RequestMapping("/api/dish-specs")
@RequiredArgsConstructor
public class DishSpecController {
    
    private final DishSpecService dishSpecService;
    
    /**
     * 获取菜品的所有规格
     */
    @GetMapping("/dish/{dishId}")
    public Result<List<DishSpec>> listByDish(@PathVariable Long dishId) {
        return Result.success(dishSpecService.listByDishId(dishId));
    }
    
    /**
     * 创建规格
     */
    @PostMapping
    public Result<DishSpec> create(@RequestBody @Valid CreateSpecRequest request) {
        return Result.success(dishSpecService.create(request));
    }
    
    /**
     * 更新规格
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateSpecRequest request) {
        dishSpecService.update(id, request);
        return Result.success();
    }
    
    /**
     * 删除规格
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishSpecService.delete(id);
        return Result.success();
    }
    
    /**
     * 批量更新菜品规格
     * Pad 端专用: 一次性保存菜品的所有规格
     */
    @PostMapping("/batch/{dishId}")
    public Result<Void> batchUpdate(
            @PathVariable Long dishId,
            @RequestBody @Valid List<SpecItemRequest> specs) {
        dishSpecService.batchUpdate(dishId, specs);
        return Result.success();
    }
    
    /**
     * 启用/禁用规格
     */
    @PostMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        dishSpecService.toggleStatus(id);
        return Result.success();
    }
}
```

### 3.2 修改后的菜品接口

```java
@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    
    private final DishService dishService;
    
    /**
     * 获取菜品详情（包含规格）
     */
    @GetMapping("/{id}")
    public Result<DishDetailDTO> getDetail(@PathVariable Long id) {
        return Result.success(dishService.getDetailWithSpecs(id));
    }
    
    /**
     * 按分类获取菜品（包含规格）
     * Pad 端点餐使用
     */
    @GetMapping("/by-category")
    public Result<List<CategoryWithDishesDTO>> listByCategory() {
        return Result.success(dishService.listByCategoryWithSpecs());
    }
    
    /**
     * 快速修改价格
     * Pad 端专用
     */
    @PostMapping("/{id}/price")
    public Result<Void> updatePrice(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePriceRequest request) {
        dishService.updatePrice(id, request);
        return Result.success();
    }
    
    /**
     * 切换是否有规格
     */
    @PostMapping("/{id}/toggle-specs")
    public Result<Void> toggleHasSpecs(@PathVariable Long id) {
        dishService.toggleHasSpecs(id);
        return Result.success();
    }
}
```

### 3.3 DTO 设计

```java
// 创建规格请求
@Data
public class CreateSpecRequest {
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;
    
    @NotBlank(message = "规格名称不能为空")
    @Size(max = 50, message = "规格名称最多50字")
    private String name;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    private Integer sortOrder = 0;
}

// 菜品详情（含规格）
@Data
public class DishDetailDTO {
    private Long id;
    private String name;
    private String description;
    private String image;
    private Integer stock;
    private Integer status;
    private Integer hasSpecs;
    private BigDecimal basePrice;  // 无规格时的基础价格
    private List<DishSpecVO> specs;  // 规格列表
}

// 规格VO
@Data
public class DishSpecVO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer status;
}

// 批量更新规格请求
@Data
public class SpecItemRequest {
    private Long id;        // 新建时为null
    private String name;
    private BigDecimal price;
    private Integer sortOrder;
    private Integer status;
}
```

---

## 4. 前端设计

### 4.1 Pad 端菜品管理页面

```
┌─────────────────────────────────────────────────────────────┐
│  菜品管理                              [分类管理] [+新增菜品] │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌─────────────────────────────────────┐  │
│  │  🍽️ 热菜     │  │ 宫保鸡丁                      [编辑] │  │
│  │  🥗 凉菜     │  │ ¥38.00                              │  │
│  │  🍲 汤羹     │  │ 规格: 小份/中份/大份        [配置规格] │  │
│  │              │  ├─────────────────────────────────────┤  │
│  │ [+新增分类]  │  │ 麻婆豆腐                    [编辑] │  │
│  │              │  │ ¥18.00                              │  │
│  └──────────────┘  │ 库存: 999                    [下架]  │  │
│                    ├─────────────────────────────────────┤  │
│                    │ ...                                 │  │
│                    └─────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 规格配置弹窗

```
┌────────────────────────────────────────┐
│ 配置规格 - 宫保鸡丁              [X]   │
├────────────────────────────────────────┤
│ 规格模式: [单价格 ○] [多规格 ●]        │
│                                        │
│ 规格列表:                              │
│ ┌──────────────────────────────────┐   │
│ │ [小份]        ¥28        [-] [↑] │   │
│ │ [中份]        ¥38        [-] [↓] │   │
│ │ [大份]        ¥48        [-]     │   │
│ └──────────────────────────────────┘   │
│                                        │
│ [+ 添加规格]                           │
│                                        │
│ 提示: 顾客点餐时需要选择规格           │
│                                        │
│        [取消]        [保存]            │
└────────────────────────────────────────┘
```

### 4.3 点餐界面适配

```
┌────────────────────────────────────────┐
│ 菜品名称                    ¥38 起     │
│ 经典川菜，鸡肉鲜嫩...                  │
│                                        │
│ 选择规格:                              │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │  小份    │ │  中份    │ │  大份    ││
│ │  ¥28     │ │  ¥38  ✓  │ │  ¥48     ││
│ └──────────┘ └──────────┘ └──────────┘│
│                                        │
│ 数量: [ - ]  1  [ + ]                  │
│                                        │
│ 备注: [______________]                 │
│                                        │
│     [加入购物车]                       │
└────────────────────────────────────────┘
```

---

## 5. Service 层实现

```java
@Service
@RequiredArgsConstructor
public class DishSpecService {
    
    private final DishSpecMapper specMapper;
    private final DishMapper dishMapper;
    
    /**
     * 获取菜品的所有规格
     */
    public List<DishSpec> listByDishId(Long dishId) {
        LambdaQueryWrapper<DishSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishSpec::getDishId, dishId)
               .eq(DishSpec::getStatus, 1)
               .orderByAsc(DishSpec::getSortOrder);
        return specMapper.selectList(wrapper);
    }
    
    /**
     * 批量更新规格（Pad 端专用）
     */
    @Transactional
    public void batchUpdate(Long dishId, List<SpecItemRequest> specs) {
        // 1. 验证菜品存在
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        
        // 2. 获取现有规格
        LambdaQueryWrapper<DishSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishSpec::getDishId, dishId);
        List<DishSpec> existingSpecs = specMapper.selectList(wrapper);
        Map<Long, DishSpec> existingMap = existingSpecs.stream()
            .collect(Collectors.toMap(DishSpec::getId, s -> s));
        
        // 3. 处理传入的规格列表
        Set<Long> processedIds = new HashSet<>();
        int sortOrder = 0;
        
        for (SpecItemRequest specReq : specs) {
            if (specReq.getId() != null && existingMap.containsKey(specReq.getId())) {
                // 更新现有规格
                DishSpec existing = existingMap.get(specReq.getId());
                existing.setName(specReq.getName());
                existing.setPrice(specReq.getPrice());
                existing.setSortOrder(sortOrder++);
                existing.setStatus(specReq.getStatus() != null ? specReq.getStatus() : 1);
                specMapper.updateById(existing);
                processedIds.add(specReq.getId());
            } else {
                // 创建新规格
                DishSpec newSpec = new DishSpec();
                newSpec.setDishId(dishId);
                newSpec.setName(specReq.getName());
                newSpec.setPrice(specReq.getPrice());
                newSpec.setSortOrder(sortOrder++);
                newSpec.setStatus(1);
                specMapper.insert(newSpec);
            }
        }
        
        // 4. 删除未包含的规格
        for (DishSpec existing : existingSpecs) {
            if (!processedIds.contains(existing.getId())) {
                specMapper.deleteById(existing.getId());
            }
        }
        
        // 5. 更新菜品 hasSpecs 标志
        dish.setHasSpecs(specs.isEmpty() ? 0 : 1);
        dishMapper.updateById(dish);
    }
    
    /**
     * 创建单个规格
     */
    public DishSpec create(CreateSpecRequest request) {
        DishSpec spec = new DishSpec();
        spec.setDishId(request.getDishId());
        spec.setName(request.getName());
        spec.setPrice(request.getPrice());
        spec.setSortOrder(request.getSortOrder());
        spec.setStatus(1);
        specMapper.insert(spec);
        
        // 更新菜品标志
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish != null && dish.getHasSpecs() == 0) {
            dish.setHasSpecs(1);
            dishMapper.updateById(dish);
        }
        
        return spec;
    }
}
```

---

## 6. 数据库迁移脚本

```sql
-- 1. 新增 dish_spec 表
CREATE TABLE IF NOT EXISTS dish_spec (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID',
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    name VARCHAR(50) NOT NULL COMMENT '规格名称',
    price DECIMAL(10,2) NOT NULL COMMENT '规格价格',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dish_id (dish_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格表';

-- 2. 修改 dish 表
ALTER TABLE dish ADD COLUMN has_specs TINYINT DEFAULT 0 COMMENT '是否有规格: 0无 1有' AFTER status;
ALTER TABLE dish ADD COLUMN base_price DECIMAL(10,2) COMMENT '基础价格(无规格时)' AFTER price;

-- 3. 迁移现有数据
-- 将现有菜品的价格作为 base_price
UPDATE dish SET base_price = price;

-- 4. 修改 order_item 表
ALTER TABLE order_item ADD COLUMN spec_id BIGINT COMMENT '规格ID' AFTER dish_id;
ALTER TABLE order_item ADD COLUMN spec_name VARCHAR(50) COMMENT '规格名称快照' AFTER dish_name;
```

---

## 7. 前端 API 封装

```typescript
// api/dishSpec.ts
import request from '@/utils/request'

export interface SpecItem {
  id?: number
  name: string
  price: number
  sortOrder?: number
  status?: number
}

// 获取菜品规格
export function getDishSpecs(dishId: number): Promise<SpecItem[]> {
  return request.get(`/dish-specs/dish/${dishId}`)
}

// 批量更新规格
export function batchUpdateSpecs(dishId: number, specs: SpecItem[]): Promise<void> {
  return request.post(`/dish-specs/batch/${dishId}`, specs)
}

// 创建规格
export function createSpec(data: { dishId: number; name: string; price: number }): Promise<SpecItem> {
  return request.post('/dish-specs', data)
}

// 更新规格
export function updateSpec(id: number, data: Partial<SpecItem>): Promise<void> {
  return request.put(`/dish-specs/${id}`, data)
}

// 删除规格
export function deleteSpec(id: number): Promise<void> {
  return request.delete(`/dish-specs/${id}`)
}

// 切换规格状态
export function toggleSpecStatus(id: number): Promise<void> {
  return request.post(`/dish-specs/${id}/toggle`)
}

// 切换菜品规格模式
export function toggleDishSpecMode(dishId: number): Promise<void> {
  return request.post(`/dishes/${dishId}/toggle-specs`)
}
```

---

## 8. 实施计划

### Phase 1: 后端改造 (2-3 天)

1. **数据库**
   - [ ] 创建 dish_spec 表
   - [ ] 修改 dish 表添加 has_specs 字段
   - [ ] 修改 order_item 表添加 spec 相关字段

2. **后端代码**
   - [ ] 创建 DishSpec 实体类
   - [ ] 创建 DishSpecMapper
   - [ ] 创建 DishSpecService
   - [ ] 创建 DishSpecController
   - [ ] 修改 DishService 支持规格查询
   - [ ] 修改 OrderService 支持规格下单
   - [ ] 修改 OrderStatusService 金额计算逻辑

3. **API 测试**
   - [ ] 规格 CRUD 接口测试
   - [ ] 批量更新接口测试
   - [ ] 带规格下单测试
   - [ ] 金额计算准确性验证

### Phase 2: Pad 端界面 (3-4 天)

1. **菜品管理页**
   - [ ] 分类列表侧边栏
   - [ ] 菜品卡片列表
   - [ ] 新增/编辑菜品弹窗
   - [ ] 图片上传组件

2. **规格配置弹窗**
   - [ ] 规格模式切换
   - [ ] 规格列表编辑
   - [ ] 拖拽排序
   - [ ] 批量保存

3. **点餐界面改造**
   - [ ] 规格选择组件
   - [ ] 价格动态显示
   - [ ] 购物车适配
   - [ ] 订单确认页适配

### Phase 3: 其他端适配 (1-2 天)

1. **Admin 端**
   - [ ] 菜品列表显示规格信息
   - [ ] 订单详情显示规格

2. **Mobile 端**
   - [ ] 规格选择界面
   - [ ] 订单详情显示规格

3. **后厨端**
   - [ ] 订单显示规格信息

### Phase 4: 测试与优化 (2-3 天)

- [ ] 全流程测试
- [ ] 性能测试
- [ ] Bug 修复
- [ ] 文档更新

---

## 9. 注意事项

### 9.1 数据一致性
- 修改规格价格不影响已下单的订单（快照机制）
- 删除规格时检查是否有未完成的订单在使用

### 9.2 兼容性问题
- 无规格菜品保持原有逻辑不变
- 订单历史数据兼容（spec_id 可为空）

### 9.3 性能考虑
- 菜品列表接口需要联查规格表
- 建议使用缓存减少数据库查询

### 9.4 边界情况
- 规格全部禁用时，菜品自动变为下架状态
- 切换规格模式时，需要清理购物车中的该菜品

---

**预计总工期:** 8-12 天
**优先级:** P0 (核心功能)
