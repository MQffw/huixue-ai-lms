# huixuetong-web（前端）

慧学通前端：Vue 3 + Vite + Element Plus + Pinia + ECharts。

> 整体启动流程（含数据库、后端）见根目录 [`README.md`](../README.md)；
> Docker 开发环境与踩坑记录见 [`docs/env/本地Docker开发环境.md`](../docs/env/本地Docker开发环境.md)。

## 开发

```bash
npm install
npm run dev          # http://localhost:5173
```

后端默认跑在 `8080`，`/api` 请求由 Vite dev server 代理过去（见 `vite.config.js`）。
后端换了地址时用环境变量覆盖，不用改代码：

```bash
VITE_PROXY_TARGET=http://localhost:18080 npm run dev
```

## 构建与检查

```bash
npm run build        # 产物在 dist/
npm run preview      # 本地预览构建结果
npm run lint         # ESLint
```

生产环境不直接跑 dev server：`Dockerfile.web` 会把 `dist/` 打进 Nginx 镜像，
由 `deploy/nginx/default.conf` 负责 `/` 静态资源与 `/api` 反代（对外只暴露 8088）。

## 目录

```
src/
├── api/          # axios 封装与接口定义
├── assets/       # 静态资源
├── components/   # 通用组件
├── composables/  # 组合式函数
├── router/       # 路由表
├── stores/       # Pinia 状态
├── utils/        # 工具函数
└── views/        # 页面
```
