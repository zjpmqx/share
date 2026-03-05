# 校园二手交易平台（Campus Second-hand Trading Platform）

一个面向校园场景的二手交易与内容分享平台，提供商品发布与交易、订单流转、站内互动、后台审核以及好物分享等功能。项目采用前后端分离架构，适合课程实践、毕业设计与中小型平台原型搭建。

## 项目状态

- 当前状态：持续开发中（可用于学习、演示与小规模部署）
- 最近更新：见 [CHANGELOG.md](./CHANGELOG.md)

## 核心功能与特性

### 用户端功能
- 用户注册、登录与身份认证（JWT）
- 二手商品发布、编辑、上下架与浏览
- 商品留言互动
- 订单生命周期管理（创建、支付、发货、确认收货、取消）
- 个人中心（我发布的商品、我的订单等）

### 好物分享模块
- 支持图文、视频、链接、附件等多种分享类型
- 分享详情与评论互动
- 分享访问口令校验（Share Gate）
- 附件上传/下载与分片上传支持

### 管理后台功能
- 商品审核与状态管理
- 用户管理
- 分享内容管理
- 后台统一布局与导航

## 技术栈与架构概述

### 前端（`campus-secondhand-web`）
- Vue 3（`<script setup>`）
- Vite 5
- Vue Router 4
- Axios
- Element Plus

### 后端（`campus-secondhand-backend`）
- Java 17
- Spring Boot 3
- Spring Security（JWT 鉴权）
- MyBatis
- MySQL 8

### 架构说明
- 前后端分离：前端通过 `/api` 调用后端 REST 接口
- 鉴权机制：JWT + 访问控制过滤器
- 文件系统存储：上传文件存储于后端配置目录，并通过 `/api/uploads/**` 提供访问
- 分享模块额外安全层：Share Gate 访问令牌校验

## 目录结构

```text
.
├─ campus-secondhand-web/        # 前端项目（Vue3 + Vite）
├─ campus-secondhand-backend/    # 后端项目（Spring Boot）
│  ├─ db/schema.sql              # 数据库初始化脚本
│  └─ src/main/resources/        # 后端配置文件
└─ CHANGELOG.md                  # 更新日志
```

## 环境要求

- Node.js 18+
- npm 9+
- Java 17+
- Maven 3.8+
- MySQL 8.0+

## 安装与启动指南

### 1）克隆仓库

```bash
git clone https://github.com/zjpmqx/share.git
cd share
```

### 2）初始化数据库

```sql
-- 在 MySQL 中执行
source campus-secondhand-backend/db/schema.sql;
```

默认数据库名建议为：`campus_trade`

### 3）配置后端

编辑：`campus-secondhand-backend/src/main/resources/application.yml`

需要重点确认：
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `app.jwt.secret`

### 4）启动后端服务

```bash
cd campus-secondhand-backend
mvn spring-boot:run
```

默认端口：`18081`

### 5）启动前端服务

```bash
cd campus-secondhand-web
npm install
npm run dev
```

默认端口：`3001`

## 使用方法与示例

### 普通用户流程
1. 注册/登录账号
2. 发布商品或浏览商品
3. 发起订单并进行交易流程
4. 在“好物分享”中查看或发布内容

### 创建管理员账号（示例）

```sql
UPDATE t_user SET role = 'ADMIN' WHERE username = '你的用户名';
```

重新登录后可访问后台管理能力。

### API 调用示例（登录）

```bash
curl -X POST http://localhost:18081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

### API 调用示例（携带 JWT）

```bash
curl http://localhost:18081/api/auth/me \
  -H "Authorization: Bearer <token>"
```

## API 文档

当前仓库未提供 Swagger/OpenAPI 在线文档。

可参考以下位置了解接口：
- 后端接口概览：`campus-secondhand-backend/README.md`
- 控制器源码：`campus-secondhand-backend/src/main/java/com/campus/trade/controller/`

建议后续补充 Swagger 以便外部集成与联调。

## 贡献指南

欢迎提交 Issue 和 Pull Request。

建议流程：
1. Fork 本仓库
2. 新建功能分支：`git checkout -b feat/your-feature`
3. 提交变更：`git commit -m "feat: 描述你的改动"`
4. 推送分支：`git push origin feat/your-feature`
5. 发起 Pull Request 并描述改动背景、影响范围和验证方式

建议贡献前先完成：
- 本地可运行验证（前后端可正常启动）
- 关键流程自测（登录、发布、交易、分享）
- 文档同步更新（必要时更新 `CHANGELOG.md`）

## 许可证信息

当前仓库根目录未包含明确的 LICENSE 文件。

在未声明开源许可证的情况下，默认视为“保留所有权利”。若你计划对外开源，建议补充标准许可证（如 MIT、Apache-2.0）。

## 联系方式

- 仓库地址：https://github.com/zjpmqx/share
- 问题反馈：请通过 GitHub Issues 提交

---

如果你是第一次接触该项目，建议从以下顺序开始阅读：
1. 本 README（总体认知）
2. `campus-secondhand-backend/README.md`（后端运行与接口）
3. `campus-secondhand-web/src/router/index.js`（前端页面入口）
