# 本地 Docker 开发环境（MySQL 8 + Redis 7）

> **状态**：现行（日常开发以本文为准） · **最后核对**：2026-11-13 · 文档索引导航见 [docs/README.md](../README.md)

> 目标：不用再手动起 MySQL、Redis 也不会在项目停止后残留。
> 只把**中间件**放 Docker，Spring Boot 仍在 IDE 里跑，改代码热重载不受影响。

## 0. 日常启动流程（TL;DR）

前提：**Docker Desktop 不设开机自启，容器 `restart: "no"`**，所有启动都靠手动触发。
好处是不开机就吃掉 400MB 内存；代价是每次开工要按顺序做三件事，顺序错了会连不上库。

### 每天开工（约 30 秒）

| 步 | 做什么 | 怎么确认成功 |
|---|---|---|
| 1 | 双击 Docker Desktop，等托盘鲸鱼图标**不再转圈** | `docker info` 能返回内容 |
| 2 | 双击 `scripts/dev-up.bat`（或手敲 `docker compose -f docker-compose.dev.yml up -d`） | 脚本打印 `[OK] MySQL healthy` |
| 3 | IDEA 里 Run `HuixuetongApplication` | 日志出现 `Started HuixuetongApplication` |
| 4 | `cd frontend && npm run dev` | 打开 http://localhost:5173 |

`scripts/dev-up.bat` 做了三件事：等 Docker 引擎就绪（最多 120 秒，不用自己盯着）、`up -d`、等 MySQL 变成 healthy。
重复双击是幂等的，不会重建数据。

**为什么必须等 Docker 就绪再起 IDEA**：自启逻辑已移除（见 3.1），容器没起时应用**不会**再去拉本机
MySQL80 / redis-server，而是直接连接失败。所以顺序只有一个正确解：容器先起、IDEA 后跑。
好消息是失败得很干脆——报 `Communications link failure`，一眼就知道是容器没起。

### 收工

四层各自独立，**关哪个都不影响别的**，按需处理：

| 层 | 怎么关 | 必须吗 |
|---|---|---|
| 公网隧道（如果开过） | 隧道窗口 **Ctrl+C**；窗口已关但地址还能打开 → `scripts\tunnel-down.bat` | **必须**，不关等于把系统挂在公网 |
| 前端 Vite | 终端里 **Ctrl+C** | 随手关，占 100MB 左右 |
| 后端 Spring Boot | IDEA 的 Stop 按钮 | 随手关，占 500MB 左右 |
| MySQL / Redis 容器 | **不用关**，见下 | 可选 |

容器留着最省事：下次开机 Docker Desktop 一启动、`dev-up.bat` 再跑一遍就是原来的数据（实测 `down` 后重启，26 张表 / 19 条学生数据完好）。

| 场景 | 命令 | 数据 |
|---|---|---|
| 想彻底释放内存 | `scripts/dev-down.bat` | 保留（命名卷里） |
| 库结构改坏了、要回到初始 dump | `scripts/dev-down.bat reset` | **清空重建** |
| 换了 `deploy/mysql-init/*.sql` | 同上 | **清空重建** |
| 只是暂停一下 | `docker compose -f docker-compose.dev.yml stop` | 保留 |

`dev-down.bat` 默认不带参数时只停不删；带 `reset` 会先弹确认再 `down -v`。
`dev-down.bat` **管不到隧道和前后端进程**——它只管容器。

### 起不来时的排查顺序

```bash
docker info                                    # 1. Docker 引擎有没有起来
docker compose -f docker-compose.dev.yml ps     # 2. 容器状态
docker compose -f docker-compose.dev.yml logs --tail=50 mysql   # 3. 库的日志
```

| 现象 | 原因 | 处理 |
|---|---|---|
| `port is already allocated` | 本机 MySQL80 服务 / redis-server 抢了 3307、6379 | `Stop-Service MySQL80 -Force`；`Get-Process redis-server \| Stop-Process -Force` |
| `Cannot connect to the Docker daemon` | Docker Desktop 没启动或还在启动中 | 等图标停止转圈后重试 |
| IDEA 起了但连不上库 | 步骤顺序反了：先 IDEA 后容器 | 停掉 IDEA 里的应用 → `up -d` → 重新 Run |
| 容器 Up 但 unhealthy | 初始化 SQL 有问题 | 看 `logs mysql`，多半是 dump 里的老语法 |

## 1. 前置

- 安装 Docker Desktop（Windows 需 WSL2；Linux 装 docker-ce + compose 插件）
- 本机 3306/3307/6379 端口空闲
- 首次启动前，本机 MySQL80 服务与 redis-server 先停掉：

```powershell
Stop-Service MySQL80 -Force
Set-Service MySQL80 -StartupType Manual
Get-Process redis-server -ErrorAction SilentlyContinue | Stop-Process -Force
```

## 2. 启动 / 停止

```powershell
docker compose -f docker-compose.dev.yml up -d      # 起 MySQL + Redis
docker compose -f docker-compose.dev.yml ps         # 看状态
docker compose -f docker-compose.dev.yml down       # 两个一起停
docker compose -f docker-compose.dev.yml down -v    # 连数据卷一起删（重置库）
```

首次 `up` 会自动执行 `deploy/mysql-init/*.sql` 初始化 `huixuetong` 库。
之后数据在命名卷 `hxt-dev-mysql` / `hxt-dev-redis` 里，`down` 不会丢。

## 3. 应用配置

`application.yml` 默认就是：

```
jdbc:mysql://127.0.0.1:3307/huixuetong   用户 huixuetong / huixuetong123456
redis: localhost:6379, db0, 无密码
```

所以**不用改任何配置**，IDE 直接启动后端即可。

### 3.1 应用不再负责启停依赖（2026-11 已移除相关代码）

原先 `application.yml` 里有 `app.mysql-auto-start` / `app.redis-auto-start`，会在启动时探测端口、
必要时 `net start MySQL80` 或拉起本机 `redis-server.exe`。**这套逻辑已整体删除**：

| 已删除 | 说明 |
|---|---|
| `config/MySqlAutoStartInitializer` | 启动 Windows MySQL80 服务 |
| `config/RedisAutoStartInitializer` | 拉起本机 redis-server 子进程 |
| `config/LocalProcessRegistry` | 应用退出时反向停止上述进程/服务 |
| `application.yml` 的 `app.*-auto-start` 段 | 配置项一并移除 |

删除原因：**本机 MySQL80 服务和容器都监听 3307**，端口探测分不出是谁。开关存在时，若容器没起，
应用会静默 `net start MySQL80` 并连到本机那套 198MB 的旧库，而你以为连的是容器——数据对不上还查不出原因。

现在依赖只有一个来源：Docker。容器没起，应用访问数据库时会明确报 `Communications link failure`，一眼看出问题。

`HuixuetongApplication.main` 现在就是标准的三行启动，没有任何自定义 initializer。

### 3.2 改代码后的生效速度

| 改什么 | 怎么生效 | 耗时 |
|---|---|---|
| `.vue` / `.js` 前端 | Vite HMR，保存即刷 | 即时 |
| `.java` 后端 | IDEA `Ctrl+F5`（Rerun） | 5-10 秒 |
| `.java` + devtools 自动编译 | IDEA `Ctrl+S`，devtools 自动重启 | 2-3 秒 |
| `Mapper.xml` | devtools 重启（`mybatis.mapper-locations` 走 classpath，需随 resources 一起复制） | 5 秒 |
| `application.yml` | Rerun | 5 秒 |
| `pom.xml` 加依赖 | Maven Reload + Rerun | 首次数十秒 |

**devtools 自动重启怎么开**：`Settings → Build, Execution, Deployment → Compiler → 勾选 Build project automatically`，
再在 `Settings → Advanced Settings` 勾选 `Allow auto-make to start even if developed application is currently running`。
之后改完 Java 保存即可自动生效，不用手动重启。

> 注意：日常开发**不需要**碰任何 `docker compose build`。镜像只在“要给别人访问 / 交付”时才构建，
> 开发阶段 Docker 只承担 MySQL + Redis 两个依赖的角色。

## 4. 连库工具

| 工具 | 地址 | 账号 |
|---|---|---|
| Navicat / DBeaver | `127.0.0.1:3307` | `huixuetong` / `huixuetong123456`（或 `root` / `Root@123456`） |
| redis-cli | `127.0.0.1:6379` | 无密码 |

## 5. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `port is already allocated` | 本机 MySQL80 或 Redis 还在跑，按第 1 节停掉 |
| 首次启动库是空的 | `deploy/mysql-init/` 里没有 `.sql`；放全量 dump 后 `down -v` 再 `up` |
| 中文乱码 | compose 已设 `utf8mb4`；dump 里需有 `SET NAMES utf8mb4` |
| 想换端口 | 改 `ports` 左侧，并同步 `application.yml` |
| 容器起不来 | `docker compose -f docker-compose.dev.yml logs mysql` |

## 6. 与部署用 compose 的区别

| 文件 | 用途 | 起什么 |
|---|---|---|
| `docker-compose.dev.yml` | 本地开发（推荐日常） | 只有 MySQL + Redis，前后端本机跑 |
| `docker-compose.debug.yml` | 全容器热更新开发 | MySQL + Redis + app(mvn run) + web(vite HMR) |
| `docker-compose.yml` | 演示/迁移（全隔离） | app + web + MySQL + Redis，只暴露 8088 |
| `docker-compose.local.yml` | 覆盖层：复用本机 jar | 配合上面的 docker-compose.yml 用 |
| `docker-compose.host.yml` | 复用宿主机中间件 | 只起 app |

## 7. 全栈部署（已实测通过）

```bash
# 1) 本机打包（走本地 .m2 + 阿里云镜像，约 40 秒）
mvn -B -DskipTests package

# 2) 用打好的 jar 建镜像（约 20 秒）
docker compose -f docker-compose.yml -f docker-compose.local.yml build

# 3) 起全套
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

验证结果（2026-11-13 实测）：

| 检查项 | 结果 |
|---|---|
| `http://localhost:8088/` | HTTP 200，Vue 页面 |
| `http://localhost:8088/health` | `{"status":"UP"}`（Nginx → app 反代通） |
| `POST /api/login` | 200，MyBatis 正常查库 |
| `huixuetong-app` 健康检查 | healthy |
| 库初始化 | 26 张表，student 19 行 |

**为什么不直接用 `docker compose build`**：Dockerfile 会在容器里 `mvn package`，要下 1GB+ 依赖，其中 spring-ai 1.0.0-M6 只能从 `repo.spring.io/milestone` 拉，国内网络实测 **23 分钟零进展**（缓存一直停在 1.25GB）。用 `docker-compose.local.yml` 复用本机 jar 可以完全绕过。
交付到干净机器 / 内网机器时，去掉 `-f docker-compose.local.yml`，走标准多阶段构建；镜像加速已内置（见第 8 节）。

## 8. 容器内构建加速（走标准 Dockerfile 时）

仓库里已有 `deploy/maven/settings.xml`（阿里云镜像 + 放行 spring milestone），
`Dockerfile` 第 14 行 `COPY deploy/maven/settings.xml /root/.m2/settings.xml` 已接上，**无需额外操作**。

两个容易踩的点：

- `mirrorOf` 必须写成 `*,!spring-milestones,!spring-snapshots`。spring-ai 1.0.0-M6 只发布在
  `repo.spring.io/milestone`，阿里云没有；漏掉排除项会直接报 `Could not find artifact`。
- 依赖缓存的挂载点是 `/root/.m2/repository`，**不能挂 `/root/.m2`**。
  挂父目录会把上面 COPY 进去的 `settings.xml` 整个遮蔽掉，等于镜像配置失效。


## 9. 方案 C：全容器热更新（不想在本机装 JDK/Node 时用）

适用场景：换一台干净机器、或者要把「正在跑的环境」直接演示给别人，但又不想每次改代码都重建镜像。

```bash
docker compose -f docker-compose.debug.yml up -d          # 首次会下 maven/node 镜像 + 依赖，约 5-10 分钟
docker compose -f docker-compose.debug.yml logs -f huixuetong-app   # 看后端启动日志
```

启动后：

| 地址 | 说明 |
|---|---|
| http://localhost:5173 | Vite Dev Server，改 Vue/JS 秒级热更新 |
| http://localhost:8080 | 后端，容器内 `mvn spring-boot:run` |
| localhost:5005 | JDWP 远程调试端口，IDEA 用 Remote JVM Debug attach |

### 改代码后的生效方式

- **前端**：保存即生效，无需任何操作（挂载 `./frontend` + HMR）。
- **后端**：
  - IDEA 里 `Build → Build Project`（Ctrl+F9），class 落到挂载的 `./target`，devtools 检测到变化自动重启（约 3 秒）；
  - 或纯命令行：`docker compose -f docker-compose.debug.yml exec huixuetong-app mvn -o -q compile`。
  - 改了 `pom.xml` 增加依赖：需 `docker compose -f docker-compose.debug.yml restart huixuetong-app`（首次要联网下载新依赖）。
- **改 `application.yml`**：同样走 devtools 重启；也可以只 `restart` 容器。

### IDEA 远程断点

`Run → Edit Configurations → + → Remote JVM Debug`，Host `localhost`，Port `5005`，然后 Debug 启动即可在容器内代码上打断点。

### 依赖下载慢

`hxt-debug-m2` / `hxt-debug-node-modules` 是命名卷，只在第一次下载。国内网络慢时先给 maven 配镜像：

```bash
docker compose -f docker-compose.debug.yml run --rm huixuetong-app \
  sh -c "mkdir -p /root/.m2 && printf '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml"
```

npm 换源：

```bash
docker compose -f docker-compose.debug.yml exec huixuetong-web npm config set registry https://registry.npmmirror.com
```

## 10. WSL / Windows 相关的坑

1. **项目在 `C:\` 上时，容器的 bind mount 走 9p 协议，IO 慢 3-10 倍**。
   日常开发够用；如果 `mvn` / `vite` 明显卡，把项目移进 WSL 内部再跑：
   ```bash
   cp -r /mnt/c/Users/mq/IdeaProjects/huixuetong-lms ~/projects/
   ```
   然后用 IDEA 的 WSL 远程开发或 `\\wsl$\Ubuntu\home\<user>\projects\huixuetong-lms` 打开。

2. **Docker Desktop 在 Windows 侧执行命令最省事**。`docker compose ...` 直接在本项目目录跑即可，不用先 `wsl`。
   只有当你把项目移进 WSL 内部时，才需要在 WSL 终端里执行。

3. **dev compose 的 MySQL 映射到 `127.0.0.1:3307`，Redis 到 `127.0.0.1:6379`**，Windows 的 IDEA / Navicat 直接连 `127.0.0.1` 就能通，不用管 WSL 的 IP。

4. **应用已不再启停依赖**（见 3.1）。早期 compose 里注入的 `APP_MYSQL_AUTO_START_ENABLED` / `APP_REDIS_AUTO_START_ENABLED` 已随代码一起清理。

## 11. 临时让别人访问（公网隧道）

不买域名、不用公网 IP、不用在路由器上做端口映射——用 Cloudflare 的临时隧道（quick tunnel）。

```bash
scripts\tunnel.bat          # 双击，或在项目根目录执行
```

它会启动一个本地进程，把 `http://localhost:5173` 反向挂到公网，输出类似：

```
https://xxxx-xxxx-xxxx.trycloudflare.com
```

把这个地址发给别人，任何网络下都能打开，自带 HTTPS。

**为什么只暴露 5173 就够**：Vite dev server 会把 `/api` 转发到 `localhost:8080`，
这个转发发生在 **Node 进程内部**（服务端代理），所以后端不需要单独暴露，一个端口就够。

**实测（2026-11-13）**

| 检查 | 结果 |
|---|---|
| 首页 `/` | HTTP 200，`<title>Vite App</title>` |
| `/src/main.js` | 200，802B |
| `/@vite/client` | 200，78KB |
| `/api/login`（经 Vite 代理到 8080） | 200，3.4s |

延迟偏高（1.3–3.4s）是因为边缘节点落在境外（本次是 lax13），链路绕了一圈，属正常。

**关闭隧道**

| 情况 | 怎么做 |
|---|---|
| 隧道窗口还在 | 在那个黑窗口按 **Ctrl+C**，打印 `[OK] Tunnel closed.` 后按任意键关闭；或直接点 × 关窗口 |
| 窗口没了但地址还能打开（进程残留） | 双击 `scripts\tunnel-down.bat`，它按镜像名找 `cloudflared.exe` 并终止 |
| 想确认真的关了 | `tasklist \| findstr cloudflared`，无输出即已停；再刷新公网地址应打不开 |

```bash
scripts\tunnel-down.bat     # 兜底关闭；没在跑时提示 "already closed"
```

注意关隧道**不会**影响前端和后端——它只是个转发进程，`npm run dev` 和 IDEA 里的应用都不受牵连。
反过来，`dev-down.bat` 也管不到隧道，两者要分别关。

**注意**

1. **地址是临时的**：进程一关就失效，每次重启 URL 都会变。要固定地址得用具名隧道（需 Cloudflare 账号 + 自己的域名）。
2. **关窗口即下线**：Ctrl+C 或关掉那个黑窗口，隧道立刻断；窗口被强杀时用上面的 `tunnel-down.bat` 兜底。
3. **等于把系统公开到公网**：`config/models.json` 里有加密的 AI Key、`/login` 对外开放。
   地址虽然随机但不算保密，演示完立刻关掉。
4. 前端必须是 `npm run dev` 跑着的状态，否则隧道返回 502。
5. cloudflared 只装在 `%USERPROFILE%\tools\cloudflared.exe`，单文件、不写注册表，删掉即卸载。

**内网直连（不装任何东西）**：同一个 WiFi / 热点下的设备，直接访问
`http://<本机局域网IP>:5173` 即可。查 IP：`ipconfig`；本机防火墙已对 Node.js 放行 Public 入站。
