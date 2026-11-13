# Huixuetong Docker 部署说明

> 目标：在已有 lease 系统（8080/8081、MySQL 8、Redis、RabbitMQ、MinIO）的虚拟机上，再部署一套 Huixuetong，且互不影响。
>
> 结论：**可以**。Docker 下中间件不会“端口打架”，因为每个 compose 项目有自己的 bridge 网络；真正需要让开的只有**宿主机端口**。

---

## 一、先选模式

| | 方案 A：全隔离 | 方案 B：复用宿主机中间件 |
|---|---|---|
| 组成 | app + web + MySQL 8 + Redis 7 | 仅 app（前端静态放 102） |
| 宿主端口 | 只占 **8088** | 占 **8082**（建议绑 127.0.0.1） |
| 中间件 | 容器内独立 MySQL/Redis | 用 101 已有的 MySQL/Redis |
| 内存增量 | 约 1.0–1.2 G | 约 0.5–0.7 G |
| 适合 | 答辩演示、迁移、怕污染现有库 | 101 内存紧张、只想加一个服务 |
| 风险 | MySQL 初始化要全量 dump | 依赖宿主机库版本/权限 |

> 101 若只有 4G，且 lease 的两个 JVM + MySQL + RabbitMQ + MinIO 都在跑，优先 **方案 B**；有 8G 再上方案 A。

---

## 二、前置检查

```bash
# 1. 是否已装 Docker
docker version || podman version

# 2. 端口占用（确认 8082/8088 没被占）
ss -lntp | grep -E ':(8080|8081|8082|8088|3306|6379)\b'

# 3. 内存/磁盘
free -m; df -h /

# 4. 外网（Huixuetong 要调 LLM API）
curl -I --max-time 8 https://api.deepseek.com
```

RHEL/CentOS 默认没有 Docker，只有 Podman：

```bash
# 路线 1：装 docker-ce（需要外网）
dnf install -y dnf-plugins-core
dnf config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl enable --now docker

# 路线 2：用 podman（无需 daemon）
dnf install -y podman podman-compose
# 注意：podman 下把 host.docker.internal 换成 host.containers.internal
```

---

## 三、数据库准备（方案 A 必做，方案 B 只需建库）

### 方案 A：导出全量 dump

本机（Windows，MySQL 5.5）执行：

```powershell
mysqldump -uroot -p123456 --default-character-set=utf8mb4 --single-transaction --routines --triggers huixuetong > deploy/mysql-init/00-huixuetong.sql
```

> **必须导全量**：仓库里的 `schema-extend.sql` 只有 AI 扩展表，没有 `emp/student/clazz/...` 业务表，空库启动会“登录成功但所有列表 500”。

如果 dump 里含 `0000-00-00 00:00:00`，compose 已用 `--sql-mode=NO_ENGINE_SUBSTITUTION` 兼容；更稳妥的做法是先把 DDL 改成 `CURRENT_TIMESTAMP`。

### 方案 B：宿主机建库建用户

```sql
CREATE DATABASE huixuetong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'huixuetong'@'%' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON huixuetong.* TO 'huixuetong'@'%';
FLUSH PRIVILEGES;
```

把全量 dump 导入 `huixuetong` 库。

---

## 四、配置

```bash
cp .env.example .env
vi .env          # 填 DB_PASSWORD / REDIS_PASSWORD / MODEL_KEY_SECRET / JWT_SECRET
```

关键项：

| 变量 | 说明 |
|---|---|
| `MODEL_KEY_SECRET` | **必须与加密 `config/models.json` 时用的密钥一致**，否则 AI Key 解出来是空串，所有对话 401 |
| `DB_PASSWORD` | 方案 A 是容器 MySQL 的 huixuetong 用户密码；方案 B 是宿主机库密码 |
| `SPRING_DATA_REDIS_DATABASE` | 固定 1，和 lease 的 DB 0 隔离 |
| `JWT_SECRET` | 换掉仓库默认值 |

同时确保 `config/models.json` 已从本机拷到部署目录（该文件被 `.gitignore` 忽略，clone 下来是没有的）。

---

## 五、启动

### 方案 A

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f huixuetong-app
```

访问：`http://<101-IP>:8088`

### 方案 B

```bash
docker compose -f docker-compose.host.yml up -d --build
```

访问：`http://127.0.0.1:8082`（容器内）；对外由 102 的 Nginx 反代。

### 102 Nginx 反代（可选）

```nginx
server {
    listen 80;
    server_name huixuetong.example.com;

    location / {
        proxy_pass http://192.168.10.101:8088;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_buffering off;
        proxy_read_timeout 300s;
        client_max_body_size 20m;
    }
}
```

> 方案 B 没有 web 容器时，把前端 `dist/` 放 102，`/api/` 反代到 `101:8082`，同样要加 `proxy_buffering off`。

---

## 六、验证

```bash
# 1. 容器状态
docker compose ps

# 2. 健康检查
curl -s http://127.0.0.1:8088/health

# 3. 登录
curl -s -X POST http://127.0.0.1:8088/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"shinaian","password":"123456"}'

# 4. AI（换 token）
curl -s -X POST http://127.0.0.1:8088/api/ai/chat/sync \
  -H "token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"有多少学员","modelType":"deepseek"}'

# 5. 知识库上传（验证 MySQL 8 下 JSON 语法不再是问题）
curl -s -X POST http://127.0.0.1:8088/api/knowledge/upload \
  -H "token: $TOKEN" -F "file=@制度.txt" -F "category=rule"

# 6. Redis 隔离：DB1 有 ai:*，DB0 仍是 lease 的 key
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" -n 1 KEYS 'ai:*' | head
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" -n 0 DBSIZE
```

---

## 七、升级 / 回滚 / 备份

```bash
# 升级（重建 app，数据卷不动）
docker compose up -d --build huixuetong-app

# 回滚：改用上一版镜像 tag
docker tag huixuetong-app:previous huixuetong-app:latest
docker compose up -d huixuetong-app

# 备份数据库（backup/ 已移出仓库，先建目录）
mkdir -p backup
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction huixuetong | gzip > backup/huixuetong-$(date +%F).sql.gz

# 备份上传文件与模型配置
tar czf backup/huixuetong-files-$(date +%F).tar.gz uploads config
```

> MySQL 初始化脚本**只在数据卷为空时执行**；改了 dump 要重新初始化：
> `docker compose down -v`（会删库，先备份！）。

---

## 八、常见坑

1. **`/actuator/env` 裸奔**：compose 已把暴露端点收敛为 `health,info`；不要在公网放开 8080。
2. **Docker 会绕过 firewalld**：`-p 8088:80` 的端口不受 firewalld 控制，建议只绑内网/用 102 反代，或用 `iptables` 额外限制。
3. **SSE 被缓冲**：Nginx 的两层（容器内 + 102）都要 `proxy_buffering off`，否则 AI 回答会“转圈很久后一次性出现”。
4. **AI Key 解不开**：`MODEL_KEY_SECRET` 与加密时不一致，表现为接口 200 但回答“AI 服务暂时不可用”，日志里是 401。
5. **日期差 8 小时**：JDBC 用 `serverTimezone=Asia/Shanghai`，容器 `TZ=Asia/Shanghai`，JVM 加 `-Duser.timezone=Asia/Shanghai`（镜像已加）。
6. **方案 B 容器连不上宿主机 MySQL**：确认 `host-gateway`、MySQL `bind-address`、以及用户 host 是 `%` 或 `172.17.%`。
7. **离线环境**：镜像可在有网机器 `docker save` 后 `docker load`；或直接用 `Dockerfile.runtime` + 已打好的 jar，减少构建依赖。
8. **内存**：两个系统 + 容器 MySQL 很容易把 4G 机器打满，先 `free -m`；JVM 已限制 `-Xmx512m`，必要时降到 384m。
