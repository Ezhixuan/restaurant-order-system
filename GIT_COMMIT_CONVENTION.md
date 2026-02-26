# Git 提交规范

**生效日期:** 2026-02-27  
**适用范围:** restaurant-order-system 项目所有提交

---

## 提交格式

```
<type>: <subject>

<body> (可选)
```

---

## Type 类型

| Type | 含义 | 使用场景 |
|------|------|---------|
| **feat** | 新功能 | 添加新功能、新页面、新接口 |
| **fix** | 修复 | 修复 Bug、修复逻辑错误 |
| **docs** | 文档 | 修改文档、添加注释、README |
| **style** | 格式 | 代码格式化、缩进、分号等（不影响功能）|
| **refactor** | 重构 | 代码重构（既不是新功能也不是修复）|
| **perf** | 性能 | 性能优化 |
| **test** | 测试 | 添加或修改测试代码 |
| **chore** | 构建 | 构建工具、依赖更新、配置文件修改 |
| **revert** | 回滚 | 撤销之前的提交 |

---

## 格式规则

### Subject (必填)
- 不超过 50 个字符
- 使用中文或英文（推荐中文）
- 首字母不大写
- 末尾不加句号
- 使用祈使语气，描述"做了什么"

### Body (可选)
- 详细描述变更内容
- 说明为什么做此变更
- 可以包含关联的 issue 或任务 ID

---

## 示例

### ✅ 正确示例

```
feat: 菜品管理添加规格配置功能

- 支持多规格/单价格模式切换
- 批量更新规格接口
- Pad 端规格配置弹窗
```

```
fix: 修复结账后桌台状态未更新问题

结账完成后自动将桌台设为"待清台"(2)
```

```
docs: 更新项目 README 和 API 文档
```

```
refactor: 优化订单查询 SQL 性能

添加索引 idx_order_status_created_at
```

### ❌ 错误示例

```
[feat] 添加功能                    # 使用了旧格式 []
feat:添加功能                       # 缺少空格
feat: 添加功能。                    # 末尾有句号
feat: 添加了功能                    # 使用了过去时
fix bug                            # 缺少 type 后的冒号
```

---

## 提交前检查清单

- [ ] 使用正确的 type
- [ ] subject 简洁明了
- [ ] type 后有空格
- [ ] 末尾无句号
- [ ] 使用祈使语气（做了什么，不是做了什么了）

---

## 参考

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
