# MySQL 8.0 安装与切换手册（Windows · C 盘 · 与 5.5 并存）

> 目标：5.5 留在 `C:\Program Files\MySQL\MySQL Server 5.5`（3306）不删；
> 8.0 装到 `C:\Program Files\MySQL\MySQL Server 8.0`（3307），数据放 `C:\ProgramData`；
> 应用用 Spring profile 切换，随时可回滚。
>
> 已随仓库提供：
> - 配置模板：[deploy/mysql80/my.ini](../deploy/mysql80/my.ini)
> - 应用 profile：[application-mysql8.yml](../src/main/resources/application-mysql8.yml)

---

## 0. 动手前必须知道的三件事

### 0.1 你机器上已有 8.0 残留

| 路径 | 状态 |
|---|---|
| `C:\Program Files\MySQL\MySQL Server 8.0` | **不存在**（二进制已删） |
| `C:\ProgramData\MySQL\MySQL Server 8.0\Data` | **存在**，以前初始化的数据目录 |
| `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini` | 存在，是旧配置（`port=3306`、严格 `sql_mode`） |

**不要复用旧 `Data`**：目录已存在（可能含旧库数据，当前账号无读权限）、权限属于旧服务、端口配置也不同。
本手册新建 `Data-hxt`，旧目录原样保留，不删不动。

### 0.2 两个实例的分工

| 实例 | 端口 | 配置 | 数据 | 用途 |
|---|---|---|---|---|
| MySQL 5.5 | 3306 | 现有 | `C:\ProgramData\MySQL\MySQL Server 5.5` | 回滚点，不动 |
| MySQL 8.0 | 3307 | `$BASE\my.ini` | `...\MySQL Server 8.0\Data-hxt` | 新开发库 |

### 0.3 全程用管理员 PowerShell

写 `C:\Program Files`、注册 Windows 服务都要管理员权限。
下面的命令都假设 **同一个管理员窗口** 里执行（变量只在该会话有效）。

```powershell
$BASE  = "C:\Program Files\MySQL\MySQL Server 8.0"
$DATA  = "C:\ProgramData\MySQL\MySQL Server 8.0\Data-hxt"
$MYSQLD = "$BASE\bin\mysqld.exe"
$MYSQL  = "$BASE\bin\mysql.exe"
```

---

## 1. 解压到 `C:\Program Files\MySQL`

从 <https://dev.mysql.com/downloads/mysql/> 下载 **Windows (x86, 64-bit), ZIP Archive**，
假设放在 `D:\downloads`。

```powershell
# 解压（ZIP 顶层目录形如 mysql-8.0.43-winx64）
$zip = Get-ChildItem D:\downloads\mysql-8.0*-winx64.zip | Select-Object -First 1
Expand-Archive -Path $zip.FullName -DestinationPath "C:\Program Files\MySQL" -Force

# 改名成与 5.5 一致风格的目录
$src = Get-ChildItem "C:\Program Files\MySQL" -Directory |
       Where-Object { $_.Name -like 'mysql-8.0*' } | Select-Object -First 1
Rename-Item $src.FullName "MySQL Server 8.0"

Test-Path $MYSQLD      # True
```

> 用 ZIP 不用 MSI：MSI 会检测到 5.5 并提示升级，风险大；ZIP 不写注册表，卸载=删目录。

---

## 2. 配置 `my.ini`

```powershell
Copy-Item deploy\mysql80\my.ini "$BASE\my.ini" -Force
New-Item -ItemType Directory -Force $DATA | Out-Null
Get-Content "$BASE\my.ini" -TotalCount 12
```

关键项：

| 配置 | 值 | 不这么设的后果 |
|---|---|---|
| `port` | 3307 | 抢 3306，5.5 起不来 |
| `character-set-server` | utf8mb4 | emoji 写 `emp_log` 报 1366 |
| `default-time-zone` | `+08:00` | 与 JDBC 的 `Asia/Shanghai` 不一致，差 8 小时 |
| `sql-mode` | `NO_ENGINE_SUBSTITUTION` | 导入 5.5 零日期数据直接失败 |
| `lower_case_table_names` | 1 | 必须在初始化前定，之后改会启动失败 |

> `my.ini` 放在 `$BASE` 下是 MySQL 的标准查找路径（`basedir\my.ini`），
> 服务和命令行都会自动读到，不需要额外加 `--defaults-file`。

---

## 3. 初始化数据目录

```powershell
& $MYSQLD --initialize-insecure --console
```

- `--initialize-insecure`：root 初始密码为空，下一步再设。
- 成功后会生成 `$DATA\ibdata1`、`$DATA\mysql\` 等；失败看 `$DATA\error.log`。
- 若报 "data directory has files"，说明 `$DATA` 非空，先清空。

---

## 4. 注册服务并启动

```powershell
& $MYSQLD --install MySQL80
net start MySQL80
```

- 服务名 `MySQL80`，与现有 `MySQL`（5.5）区分。
- 想先排错可以前台跑（不注册服务）：`& $MYSQLD --console`，窗口别关。
- 停服务：`net stop MySQL80`；卸载：`& $MYSQLD --remove MySQL80`。

---

## 5. 设密码 + 建库建用户

```powershell
& $MYSQL --host=127.0.0.1 --port=3307 --user=root
```

```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'Root@123456';

CREATE DATABASE huixuetong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER 'huixuetong'@'%' IDENTIFIED BY 'huixuetong123456';
GRANT ALL PRIVILEGES ON huixuetong.* TO 'huixuetong'@'%';
FLUSH PRIVILEGES;
```

> 8.0 默认 `caching_sha2_password`，不要降级；JDBC 串里加 `allowPublicKeyRetrieval=true` 即可（profile 已配）。

---

## 6. 迁移数据（5.5 → 8.0）

### 6.1 清掉唯一一处零日期默认值（在 5.5 上执行）

已核对 `information_schema`，全库只有 `hxt_user_ai_profile.update_time` 一处：

```powershell
& "C:\Program Files\MySQL\MySQL Server 5.5\bin\mysql.exe" `
  --host=127.0.0.1 --user=root --password=123456 --database=huixuetong `
  -e "ALTER TABLE hxt_user_ai_profile MODIFY update_time TIMESTAMP NULL DEFAULT NULL;"
```

### 6.2 导出

```powershell
$out = "C:/Users/mq/IdeaProjects/huixuetong-lms/backup/huixuetong_5.5_20261108.sql"
& "C:\Program Files\MySQL\MySQL Server 5.5\bin\mysqldump.exe" `
  --host=127.0.0.1 --port=3306 --user=root --password=123456 `
  --default-character-set=utf8mb4 --single-transaction --routines --triggers `
  --databases huixuetong `
  --result-file=$out
```

> **不要用 `>` 重定向**：Windows PowerShell 5.1 的 `>` 默认写 UTF-16（文件头 `FF FE`），
> mysql 会报 `ASCII '\0' appeared in the statement`。必须用 `--result-file` 直接写字节。
> 校验文件头：`([System.IO.File]::ReadAllBytes($out))[0..1]` 应为 `2D 2D`（即 `--`）。

### 6.3 检查 `sql_mode`（本机无需处理）

```powershell
Select-String backup\huixuetong_full_5.5.sql -Pattern 'SQL_MODE' | Select-Object -First 2
```

- 出现 `NO_AUTO_VALUE_ON_ZERO`：8.0 兼容，直接下一步。**你本机就是这种。**
- 若出现 `NO_AUTO_CREATE_USER`：8.0 已删除该变量，需替换：

```powershell
$src = (Resolve-Path backup\huixuetong_full_5.5.sql).Path
$dst = Join-Path (Get-Location) 'backup\huixuetong_for80.sql'
$c = [System.IO.File]::ReadAllText($src, [System.Text.Encoding]::UTF8)
$c = $c -replace 'NO_AUTO_CREATE_USER,', ''
[System.IO.File]::WriteAllText($dst, $c, (New-Object System.Text.UTF8Encoding($false)))
```

### 6.4 导入

```powershell
& $MYSQL --host=127.0.0.1 --port=3307 --user=root --password=Root@123456 `
  --default-character-set=utf8mb4 `
  -e "source C:/Users/mq/IdeaProjects/huixuetong-lms/backup/huixuetong_full_5.5.sql"
```

> 用 `source`，不要用 `<`（PowerShell 不支持），也不要用 `Get-Content | mysql`（会破坏 UTF-8 中文）。
> 若执行过 6.3 的替换，把路径换成 `huixuetong_for80.sql`。

### 6.5 两个已踩过的坑（2026-11-08 实测）

**坑 1：dump 文件是 UTF-16**

`mysqldump ... > file.sql` 在 Windows PowerShell 5.1 下写出的是 UTF-16（头 `FF FE`），
导入报：

```
ERROR: ASCII '\0' appeared in the statement, but this is not allowed unless option --binary-mode is enabled
```

改用 `--result-file`（见 6.2）。临时判断：

```powershell
$b = [System.IO.File]::ReadAllBytes("backup\huixuetong_5.5_20261108.sql")[0..1]
($b | ForEach-Object { $_.ToString('X2') }) -join ' '     # 2D 2D 才对；FF FE 就是 UTF-16
```

**坑 2：5.5 的列注释里有 GBK 历史字节，8.0 拒绝**

报错：

```
ERROR 4088 (HY000): Comment for field 'huixuetong.emp_log.operate_emp_id'
contains an invalid utf8mb3 character string: '\xB2\xD9\xD7'
```

`\xB2\xD9\xD7` 是 GBK 的"操作"——当年用 GBK 客户端建表时写进注释的。
注释不影响运行，导入前剥掉即可：

```powershell
$src = (Resolve-Path backup\huixuetong_5.5_20261108.sql).Path
$dst = Join-Path (Get-Location) 'backup\huixuetong_for80.sql'
$lines = [System.IO.File]::ReadAllLines($src, [System.Text.Encoding]::UTF8)
$out = New-Object System.Collections.Generic.List[string]
foreach ($l in $lines) {
    if ($l -match '^\s+`[^`]+`\s' -and $l -match "COMMENT\s+'") {
        $l = [regex]::Replace($l, "\s*COMMENT\s+'(?:[^']|'')*'", '')
    } elseif ($l -match '^\)\s*ENGINE' -and $l -match "COMMENT=") {
        $l = [regex]::Replace($l, "\s*COMMENT='(?:[^']|'')*'", '')
    }
    $out.Add($l)
}
[System.IO.File]::WriteAllLines($dst, $out, (New-Object System.Text.UTF8Encoding($false)))
```

本次共剥离 192 个列注释、26 个表注释。

---

## 7. 应用启动

**默认配置已指向 8.0**，不再需要 `mysql8` profile：

```powershell
mvn spring-boot:run
```

`application.yml` 里已经写好：

1. 数据源 → `127.0.0.1:3307/huixuetong`，`utf8mb4` + `Asia/Shanghai` + `allowPublicKeyRetrieval=true`；
2. Redis `localhost:6379`，无密码、db0（2026-11 起应用侧的自动启停逻辑已移除，依赖统一由 Docker 提供）；
3. `server.servlet.encoding.force=true`，避免 multipart 中文文件名乱码。

`application-mysql8.yml` 保留为空文件，只为兼容旧命令
`mvn spring-boot:run "-Dspring-boot.run.profiles=mysql8"`，它不再覆盖任何配置。

Redis 手动启动（自动拉起失败时）：

```powershell
D:\Redis-8.8.0-Windows-x64-cygwin\redis-server.exe D:\Redis-8.8.0-Windows-x64-cygwin\redis.conf
```

---

## 8. 环境变量：要不要加？

**结论：PATH 不加。** 原因：

- 你机器上会有两套 `bin`（5.5 和 8.0），`mysql` / `mysqld` / `mysqladmin` 都会指向不明；
- 应用走 JDBC 直连，**不依赖 PATH**；
- 命令里用 `$MYSQL` / `$MYSQLD` 变量已经足够短。

可选：给自己加一个**专用变量**（不污染 PATH）：

```powershell
[Environment]::SetEnvironmentVariable('MYSQL80_HOME', $BASE, 'User')
# 新开窗口后：
& "$env:MYSQL80_HOME\bin\mysql.exe" --host=127.0.0.1 --port=3307 -uroot -p
```

如果你确实想加 PATH（例如常用命令行客户端）：

```powershell
$old = [Environment]::GetEnvironmentVariable('Path','User')
[Environment]::SetEnvironmentVariable('Path', "$BASE\bin;$old", 'User')
```

注意两点：

1. 放在**最前面**，`mysql` 才会是 8.0 客户端（8.0 客户端可连 5.5 服务端，向下兼容）；
2. 即使加了，`mysql` 默认仍连 3306，**查 8.0 必须带 `-P3307`**。

应用侧真正相关的环境变量不是 PATH，而是：

| 变量 | 作用 | 现在是否必须 |
|---|---|---|
| `DB_PASSWORD` | 覆盖 profile 里的数据库密码 | 否（有默认值） |
| `MODEL_KEY_SECRET` | 解密 `config/models.json` 里的 API Key | 本机可空（用开发密钥，日志会 WARN） |
| `JWT_SECRET` | 签发 token | 本机可空，生产必须设 |

---

## 9. 验证清单

**数据库**

```sql
SELECT VERSION();                                        -- 8.0.x
SELECT @@datadir, @@port;                                -- ...Data-hxt\ / 3307
SELECT @@character_set_server, @@collation_server;       -- utf8mb4 / utf8mb4_general_ci
SELECT @@sql_mode, @@time_zone;                          -- 无 STRICT / +08:00
SELECT COUNT(*) FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='huixuetong';                        -- 26
SELECT COUNT(*) FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='huixuetong' AND TABLE_COLLATION NOT LIKE 'utf8mb4%';  -- 0
SELECT COUNT(*) FROM huixuetong.attendance;              -- 33
SELECT COUNT(*) FROM huixuetong.emp WHERE role='ADMIN';  -- 1
```

**应用**

- [ ] 启动无异常，`/actuator/health` 的 `db` 为 UP
- [ ] `POST /login`（shinaian/123456）返回 `code=1`
- [ ] 学生/班级列表中文不乱码
- [ ] 写一条带 emoji 的记录，`emp_log` 能落库

---

## 10. 回滚 / 卸载

```powershell
net stop MySQL80          # 停 8.0
mvn spring-boot:run       # 不带 profile，回到 3306 的 5.5
```

彻底卸载 8.0（保留 5.5 不受影响）：

```powershell
net stop MySQL80
& $MYSQLD --remove MySQL80
Remove-Item "C:\Program Files\MySQL\MySQL Server 8.0" -Recurse -Force
# 数据目录想清再手动删：C:\ProgramData\MySQL\MySQL Server 8.0\Data-hxt
```

旧残留 `C:\ProgramData\MySQL\MySQL Server 8.0\Data` 不要动，与你新实例无关。

---

## 11. 常见错误

| 报错 | 原因 | 处理 |
|---|---|---|
| `ERROR 2003 Can't connect` | 服务没起 / 端口错 | `net start MySQL80`；确认 3307 |
| `Access denied for user 'root'@'localhost'` | 初始密码不是空 / 连错实例 | 先无密码登录；确认 `-P3307` |
| `Public Key Retrieval is not allowed` | 8.0 默认 caching_sha2 | JDBC 加 `allowPublicKeyRetrieval=true` |
| `data directory has files` | 复用了非空目录 | 换 `Data-hxt` 或清空 |
| 服务启动失败 1067 | my.ini 路径/端口/字符集写错 | 看 `$DATA\error.log` 第一段 |
| `unknown variable 'mysqlx=0'` | 个别版本不认 | my.ini 里保持注释 |
| 时间差 8 小时 | 时区不一致 | `+08:00` + `serverTimezone` |
| `mysql -e "中文"` 报 1366 | Windows 控制台按 GBK 传参 | 中文 SQL 写成 UTF-8 文件后 `source` |
| 缺 `VCRUNTIME140.dll` | 未装 VC++ 运行库 | 装 Microsoft Visual C++ 2015-2022 Redistributable |

---

## 12. 附：Docker 备选（答辩/部署）

```yaml
# docker-compose.dev.yml
services:
  mysql:
    ports: ["127.0.0.1:3307:3306"]
  redis:
    ports: ["127.0.0.1:6380:6379"]
```

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql redis
```

前提：Docker Desktop + WSL2（`wsl --install --no-distribution`，需重启一次）。

---

## 13. 本次迁移记录（2026-11-08 实测）

| 项 | 结果 |
|---|---|
| 版本 | MySQL **8.0.46**（ZIP，`mysql-8.0.46-winx64.zip`） |
| basedir | `C:\Program Files\MySQL\MySQL Server 8.0` |
| datadir | `C:\ProgramData\MySQL\MySQL Server 8.0\Data-hxt` |
| 端口 | 3307（5.5 仍在 3306，未改动） |
| 服务 | `MySQL80`，Automatic，Running |
| root | `Root@123456` |
| 业务账号 | `huixuetong@%` / `huixuetong123456` |
| 参数 | `utf8mb4 / utf8mb4_general_ci / +08:00 / NO_ENGINE_SUBSTITUTION` |

**数据核对（5.5 与 8.0 完全一致）**

| 表 | 行数 | 表 | 行数 |
|---|---|---|---|
| emp | 32 | score | 31 |
| student | 19 | exam | 12 |
| clazz | 9 | payment | 30 |
| course | 16 | notice | 11 |
| attendance | 33 | emp_log | 320 |
| hxt_ai_chat_record | 125 | hxt_token_usage | 757 |

共 **26 张表**，非 utf8mb4 表 **0**；`emp.role='ADMIN'` 1 人（shinaian）；
中文完整性抽查：班级名含"班" 2 条、员工名含"宋" 1 条。

**能力验证**

- emoji 写入 `emp_log`：通过（`emoji_rows = 1`）
- `CAST('{...}' AS JSON)`：通过
- `FULLTEXT ... WITH PARSER ngram` 中文检索：通过（`ngram_hits = 1`）

**应用验证**（`-Dspring-boot.run.profiles=mysql8`）

- 启动 7.3 秒；`POST /login` → `code=1`
- `GET /students?page=1&pageSize=3` → `code=1, total=19`
- `/actuator/health`：`db=UP`、`ai=UP`、`redis=DOWN`（未启动，需手动起 Redis）

> 旧 8.0 残留已清理：`C:\ProgramData\MySQL\MySQL Server 8.0`（172 个文件 / 179 MB）删除，
> 5.5 的目录、服务、注册表均未改动。
