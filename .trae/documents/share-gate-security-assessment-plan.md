# 好物分享口令验证安全评估计划

## 目标
对整个项目中的“好物分享口令验证”机制做一次授权的安全测试规划，后续实施阶段将以“攻击者视角”模拟破解与绕过，但仅限于本地/受控环境，不做破坏性操作，不影响业务数据，不尝试越权扩散到无关模块。

本次计划重点验证以下问题：
1. 口令本身是否容易被暴力猜解
2. 口令验证接口的限流与封禁是否可被绕过
3. 分享访问令牌是否可伪造、复用、窃取或长期滥用
4. 前端路由守卫与本地存储方案是否只是“表面防护”
5. 服务端是否真正构成有效边界
6. 配置与部署方式是否会削弱整套分享门禁机制

## 已识别实现链路
### 前端
- `campus-secondhand-web/src/views/ShareVerify.vue`
  - 输入口令并调用 `/api/auth/share-gate/verify`
  - 成功后将 token 写入本地存储
- `campus-secondhand-web/src/router/index.js`
  - 访问 `/shares` 路由时若本地无 token，则跳转验证页
- `campus-secondhand-web/src/services/auth.js`
  - 使用 `localStorage` 保存 `campus_share_gate_token`
- `campus-secondhand-web/src/services/http.js`
  - 请求 `/shares` 接口时自动附带 `X-Share-Gate-Token`
  - 收到 403 且消息为 `Share gate verify required` 时清理本地 token 并跳转验证页

### 后端
- `campus-secondhand-backend/src/main/java/com/campus/trade/controller/AuthController.java`
  - `POST /api/auth/share-gate/verify`
  - 基于 IP 做请求窗口限流与失败封禁
  - 验证通过后生成 share gate token
- `campus-secondhand-backend/src/main/java/com/campus/trade/security/ShareGateFilter.java`
  - 对 `/api/shares` 统一校验 `X-Share-Gate-Token`
- `campus-secondhand-backend/src/main/java/com/campus/trade/security/JwtService.java`
  - 生成 subject=`share-gate`、scope=`SHARE_GATE` 的 JWT
- `campus-secondhand-backend/src/main/resources/application-example.yml`
  - 分享门禁相关配置项：口令、过期时间、频率限制、失败封禁

## 初步安全判断
当前实现并不是“只有前端跳转拦截”，后端确实存在真实的服务端门禁，这是积极信号；但仍有若干明显风险点需要实测：

1. **口令验证接口是核心薄弱点**
   - 只要 `/api/auth/share-gate/verify` 被猜中一次，即可换取一个可直接访问 `/api/shares` 的 token。
2. **限流和封禁依赖 IP，且信任转发头**
   - `extractIp()` 优先读取 `X-Forwarded-For` 和 `X-Real-IP`。
   - 在未经过可信代理清洗头部的情况下，攻击者可能伪造来源 IP，从而规避基于 IP 的限流与封禁。
3. **分享门禁 token 保存在 localStorage**
   - 若站内存在 XSS，则可直接窃取该 token。
4. **分享门禁 token 与用户、设备、会话未绑定**
   - 任意拿到 token 的客户端都可能复用。
5. **JWT 与普通登录 JWT 使用同一密钥**
   - 虽然 claim 区分了 scope，但若密钥泄露，两个体系都会受影响。
6. **门禁本质上依赖“共享口令”**
   - 如果口令强度不足，哪怕有限流，仍可能在足够长时间内被猜出。

## 实施阶段的测试策略
后续如果你确认执行，我会按以下顺序进行，尽量先做低风险验证，再做更接近攻击者视角的模拟。

### 第一阶段：全链路基线验证
目标：确认门禁的真实边界，避免误把前端行为当作安全措施。

步骤：
1. 启动前后端并确认分享模块入口与接口连通性。
2. 不经过前端页面，直接请求 `/api/shares`：
   - 无 `X-Share-Gate-Token`
   - 伪造无效 token
   - 过期 token
3. 验证后端是否稳定返回 403 与统一错误消息。
4. 验证 `/api/auth/share-gate/verify` 成功后签发的 token 能否独立访问 `/api/shares`。

预期输出：
- 门禁是否真正由后端控制
- 前端路由守卫是否只是体验层引导

### 第二阶段：暴力猜解模拟
目标：站在攻击者视角评估口令是否可被现实地猜中。

步骤：
1. 收集页面文案、项目命名、默认配置痕迹、部署习惯等弱口令线索。
2. 构造分层字典，不做无限制全量爆破：
   - 项目相关词
   - 校园二手/分享/测试环境常见口令
   - 中文拼音、大小写变体、数字后缀
3. 以低速、受控速率请求 `/api/auth/share-gate/verify`。
4. 记录：
   - 每分钟允许的尝试次数
   - 触发封禁的阈值
   - 封禁持续时间
   - 封禁期间返回信息
5. 判断在当前限制下，弱口令是否仍可在可接受成本内被猜出。

预期输出：
- 口令强度风险结论
- 现有限流参数的有效性评估

### 第三阶段：限流与封禁绕过测试
目标：验证当前按 IP 防护是否可靠。

步骤：
1. 测试连续错误请求是否按预期触发封禁。
2. 在受控环境下，改变 `X-Forwarded-For` / `X-Real-IP`：
   - 单个伪造 IP
   - 多个轮换 IP
   - 多值 `X-Forwarded-For`
3. 观察服务端是否将伪造头部视为真实来源。
4. 若可绕过，则评估攻击者通过伪造来源头实现分布式字典攻击的可行性。

预期输出：
- 是否存在“客户端自报 IP 即可绕过限流”的问题
- 该问题的危险等级与利用前提

### 第四阶段：令牌安全性测试
目标：验证分享门禁 token 是否能被轻易复用或滥用。

步骤：
1. 获取一次合法 token 后，在不同请求上下文中复用。
2. 测试 token 是否与登录态强绑定，或仅凭 token 即可访问分享接口。
3. 测试 token 过期前是否可无限重放。
4. 检查 token 载荷信息：
   - subject
   - scope
   - exp
5. 评估令牌泄露后的影响范围。

预期输出：
- token 是否属于“拿到即可长期通行”的共享凭证
- 泄露后的风险等级

### 第五阶段：前端存储与浏览器侧风险评估
目标：确认前端是否存在使 token 更易泄露的设计。

步骤：
1. 审查 `localStorage` 持久化策略。
2. 检查分享 token 清理时机是否充分。
3. 检查是否存在容易配合 XSS 导致 token 泄露的入口。
4. 区分“前端可见 token”与“服务端强制验证”各自的安全边界。

预期输出：
- localStorage 带来的实际风险说明
- 是否需要改为更安全的承载方式

### 第六阶段：配置与部署风险检查
目标：识别配置层导致的削弱点。

步骤：
1. 检查是否仍使用示例配置中的弱默认值。
2. 检查 JWT secret 是否足够强。
3. 检查 share gate passphrase 是否可能沿用默认/测试口令。
4. 检查反向代理是否正确覆盖并清洗转发头。
5. 检查跨域与公开资源配置是否间接暴露分享内容。

预期输出：
- 配置层高风险项列表
- 部署前必须整改的安全项

## 执行时将产出的结果
实施结束后，我会给出：
1. 攻击路径复盘
2. 已验证可利用的问题列表
3. 未验证但存在迹象的问题列表
4. 风险分级（高/中/低）
5. 每个问题的复现条件
6. 修复建议与优先级
7. 如你需要，我还可以继续直接修补代码并补上测试

## 预期重点发现
基于当前代码，最值得优先验证的点是：
1. `X-Forwarded-For` / `X-Real-IP` 可否伪造导致限流失效
2. 共享口令是否过弱，是否能被字典攻击在现实条件下猜中
3. 分享 token 泄露后是否可跨设备复用
4. localStorage 是否放大 XSS 后果
5. 配置是否存在默认值/弱密钥/测试口令残留

## 执行边界
后续真正开始模拟攻击时，我会遵循以下边界：
- 仅针对本项目“好物分享口令验证”相关接口
- 仅在本地或授权环境下进行
- 不做拒绝服务型高并发压测
- 不破坏数据库、不篡改业务数据
- 不越过当前任务目标去攻击其他功能模块

## 实施顺序
1. 验证门禁边界
2. 验证暴力猜解可行性
3. 验证限流绕过
4. 验证 token 复用风险
5. 验证浏览器侧泄露风险
6. 输出结论与修复建议
