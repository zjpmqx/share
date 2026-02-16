# campus-secondhand-backend

## 1. 环境要求

- Java 17
- Maven 3.8+
- MySQL 8.0

## 2. 初始化数据库

1. 在 MySQL 中执行：

- `db/schema.sql`

2. 默认数据库名：`campus_trade`

## 3. 修改数据库连接

文件：`src/main/resources/application.yml`

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

## 4. 启动后端

在项目根目录执行（需要你在终端里进入该目录后运行）：

- `mvn spring-boot:run`

默认端口：`8080`

## 5. 接口概览（MVP）

### 5.1 认证

- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录（返回 JWT）
- `GET /api/auth/me` 获取当前用户信息（需要 Authorization 头）

JWT 用法：

- 请求头：`Authorization: Bearer <token>`

### 5.2 商品

- `GET /api/items` 商品列表（默认 `status=ON_SALE`）
- `GET /api/items/{id}` 商品详情
- `POST /api/items` 发布商品（需要登录，发布后状态 `PENDING_REVIEW`）
- `PUT /api/items/{id}` 编辑商品（仅 `PENDING_REVIEW/REJECTED` 可编辑）
- `DELETE /api/items/{id}` 删除商品（仅 `PENDING_REVIEW/REJECTED` 可删除）

### 5.3 留言

- `GET /api/items/{itemId}/messages` 留言列表
- `POST /api/items/{itemId}/messages` 发表留言（需要登录）

### 5.4 订单（更细状态）

- `POST /api/orders` 创建订单（买家，商品会被锁定）
- `GET /api/orders/my/buy` 我买到的订单
- `GET /api/orders/my/sell` 我卖出的订单
- `POST /api/orders/{id}/pay` 买家模拟付款（PENDING_PAYMENT -> PENDING_SHIPMENT）
- `POST /api/orders/{id}/ship` 卖家发货（PENDING_SHIPMENT -> PENDING_RECEIPT）
- `POST /api/orders/{id}/confirm` 买家确认收货（PENDING_RECEIPT -> COMPLETED，同时商品变 SOLD）
- `POST /api/orders/{id}/cancel` 买家取消（仅 PENDING_PAYMENT 可取消，商品回 ON_SALE）

### 5.5 后台审核（需要 ADMIN）

- `GET /api/admin/items/pending` 待审核商品列表
- `POST /api/admin/items/{id}/audit` 审核商品
  - `{ "action": "APPROVE" }` 通过（上架 ON_SALE）
  - `{ "action": "REJECT", "reason": "xxx" }` 驳回（REJECTED）

## 6. 创建管理员账号

MVP 阶段没有单独的“创建管理员”接口，你可以用 SQL 将某个用户升级为管理员。

1. 先用注册接口创建一个普通账号
2. 在 MySQL 执行：

```sql
UPDATE t_user SET role = 'ADMIN' WHERE username = '你的用户名';
```

然后重新登录获取 token，即可调用 `/api/admin/**`。
