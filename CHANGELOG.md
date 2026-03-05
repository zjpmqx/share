# 更新日志

## 2026-03-05

### 主要功能与修复
- 优化“好物分享”附件下载链路：在本地开发环境下将 `/api/uploads/**` 资源优先直连后端服务（`18081`），减少经由前端开发代理的额外转发开销，提升大文件下载体验。
- 后台管理端页面进行一组界面与交互更新，涉及侧边栏、布局与多个管理列表页面（商品/审核/分享/用户）。
- 清理项目根目录中的 `system.json` 文件。
- 后端文件控制器进行了小幅调整（删除少量无效内容）。

### 关键文件与代码模块
- 前端下载链路与资源地址归一化：
  - `campus-secondhand-web/src/services/api.js`
- 后台管理端：
  - `campus-secondhand-web/src/components/AdminSidebar.vue`
  - `campus-secondhand-web/src/layouts/AdminLayout.vue`
  - `campus-secondhand-web/src/views/AdminItems.vue`
  - `campus-secondhand-web/src/views/AdminPending.vue`
  - `campus-secondhand-web/src/views/AdminShares.vue`
  - `campus-secondhand-web/src/views/AdminUsers.vue`
- 后端文件模块：
  - `campus-secondhand-backend/src/main/java/com/campus/trade/controller/FileController.java`
- 其他：
  - 删除 `system.json`

### 依赖变更
- 本次未新增依赖。
- 本次未移除依赖。

### 用户影响说明
- 本地开发环境下，分享附件下载速度与稳定性将有所提升，特别是较大文件下载时更明显。
- 线上生产环境保持原有兼容行为，不会因该项优化导致下载地址失效。
- 后台管理页的视觉样式和部分交互体验发生变化，管理员使用体验更统一。

### 迁移与使用说明
- 若需自定义上传资源域名，可在前端环境变量中配置 `VITE_UPLOAD_ORIGIN`，例如：
  - `VITE_UPLOAD_ORIGIN=https://static.example.com`
- 若未配置该变量，开发环境会根据当前访问主机自动推导为 `http(s)://<host>:18081` 访问上传资源。
- 生产部署无需强制迁移；如有独立静态资源域名，建议补充 `VITE_UPLOAD_ORIGIN` 以获得更稳定的下载链路。
