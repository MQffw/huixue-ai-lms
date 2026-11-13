# 把项目接口批量导入 Apifox 并搭建自动化测试

适用项目：`huixuetong-lms`（慧学通，Spring Boot 3.5.13 + springdoc-openapi 2.8.6）
当前状态：脚本已就绪，静态解析口径下为 **104 个接口 / 77 条路径 / 29 个数据模型**。

---

## 0. 整体链路

```
运行中的后端（mvn spring-boot:run）
        │  /v3/api-docs + 3 个分组
        ▼
export_openapi.py（合并分组 + 注入 servers/鉴权/示例值）
        │
        ▼
apifox/openapi.json ──► 导入 Apifox（接口资产）
                                │
                                ▼
                        测试用例（AI 生成 / 手动）
                                │
                                ▼
                        自动化测试场景（编排 + 断言）
                                │
                                ▼
                        apifox run（CLI / CI 卡点）
```

关键认知：**OpenAPI 只能导入「接口定义」，不能导入「测试用例」**。
测试用例要在 Apifox 里基于接口定义生成（AI 生成 / 一键生成 / 手动添加），再组成自动化测试场景。

---

## 1. 第一步：拿到 OpenAPI 文件（二选一）

### 方式 A：从运行中的后端导出（推荐，准确度最高）

项目已引入 `springdoc-openapi-starter-webmvc-ui`，且 `/v3/api-docs/**` 已在 `WebConfig` 里被
`TokenInterceptor` 放行，**无需 token 即可访问**。

**① 启动后端**

```bash
mvn spring-boot:run
# 或直接在 IDE 里跑 HuixuetongApplication
```

**② 一条命令导出（推荐，已跑通）**

```bash
python tools/export_openapi.py --wait 120
# 后端刚启动还在加载时，--wait 会自动轮询等待就绪，最长等 120 秒
```

脚本做的事：

| 步骤 | 说明 |
|------|------|
| 自动发现分组 | 先读 `/v3/api-docs/swagger-config`，把「AI智能助手 / 业务管理 / 知识库管理」+ 默认文档**全部拉取并合并**，不会漏接口（中文分组名自动 URL 编码） |
| 注入 servers | 写入 `http://localhost:8080`，导入 Apifox 后自带前置 URL |
| 注入鉴权 | 全局 `apiKey` → header `token`；`/login` 自动排除鉴权 |
| 回填示例值 | springdoc 默认不输出 `example`，脚本按源码 POJO 字段回填 29 个模型的示例值，导入后请求体直接可跑 |

常用参数：

```bash
--base-url http://localhost:8080   # 服务地址
--out apifox/openapi.json          # 输出文件
--group 业务管理                    # 只导某一个分组（默认导出全部分组）
--no-examples                      # 不回填示例值
```

**③ 不用脚本的话，直接 curl 也行**

```bash
curl http://localhost:8080/v3/api-docs -o apifox/openapi.json
```

> ⚠️ 注意：本项目在 `application.yml` 里配了 `springdoc.group-configs`（3 个分组），
> 直接拉 `/v3/api-docs` 可能只拿到默认组。分组地址是：
> `http://localhost:8080/v3/api-docs/AI智能助手`、
> `http://localhost:8080/v3/api-docs/业务管理`、
> `http://localhost:8080/v3/api-docs/知识库管理`
> （完整列表见 `http://localhost:8080/v3/api-docs/swagger-config`）。
> curl 拉中文分组名要自己 URL 编码，比较麻烦，**所以用上面的脚本更省事**。
> 另外 curl 拿到的原始文件没有 servers / 鉴权 / 示例值，导入后这三项要手动补。

**④ 或者让 Apifox 直接拉 URL（省掉文件这一步）**

项目设置 → 导入数据 → OpenAPI/Swagger → **URL**，填 `http://localhost:8080/v3/api-docs`。
想长期同步就选「数据源绑定」，设置同步频率，接口变更自动跟进。

限制：本机 Apifox 客户端能访问 localhost；**Apifox 网页版 / 云端项目访问不到你的 localhost**，
这种情况要么用文件导入，要么做内网穿透。

### 方式 B：离线静态解析（后端起不来时的兜底，已跑通）

```bash
python tools/gen_openapi.py --src src/main/java --out apifox/openapi.json --base-url http://localhost:8080
```

脚本从源码解析 `@RestController` / `@XxxMapping` / `@Operation` / POJO 字段，自动补齐：

| 能力 | 说明 |
|------|------|
| 参数识别 | `@PathVariable` → path 参数；`@RequestParam` / 无注解 POJO → query 参数；`@RequestBody` → JSON body；`MultipartFile` → multipart/form-data |
| 数据模型 | 解析 `pojo/` 下 29 个类，含父类字段继承，生成 `$ref` 引用 |
| 示例值 | 每个字段带 `example`，导入后 Apifox 直接填充请求体，省去手填 |
| 鉴权 | 全局 `apiKey` → header `token`（本项目 JWT 走 `token` 头，不是 `Authorization`）；`/login` 自动排除鉴权 |
| 分组 | 按 `@Tag` 分模块，无注解的 Controller 已映射中文名 |

> **脚本附带了一个源码体检**：发现 `ReportController.getEmpGenderData` 的映射写成
> `@GetMapping("empGenderData")`（缺前导 `/`），实际暴露路径是 `/reportempGenderData`，
> 而前端 `frontend/src/api/report.js` 调的是 `/report/empGenderData` → **该接口必然 404**。
> 导入 Apifox 后跑一遍就能复现，修后端加个 `/` 即可。

---

## 2. 第二步：导入 Apifox

### 界面操作（最省事）

1. Apifox 新建/打开项目 → **项目设置 → 导入数据**
2. 选 **OpenAPI / Swagger** → 上传 `apifox/openapi.json`（或粘贴 URL）
3. 导入选项勾上「同步接口分组 / 覆盖已有接口」（首次导入用默认即可）
4. 完成后在「接口管理」按 tag 校验：登录认证、部门管理、员工管理、学员管理、班级管理、课程管理、考勤管理、成绩管理、缴费管理、就业管理、通知公告、违纪管理、知识库、AI智能助手、模型注册表、AI统计、数据统计

### CLI 操作（可脚本化 / 上 CI）

```bash
npm i -g apifox-cli@latest --registry=https://registry.npmmirror.com/
apifox auth login --with-token <API访问令牌>   # 头像 → 账号设置 → API 访问令牌
apifox project list                            # 拿到 projectId

# 在项目根目录建 .apifox/settings.json 写入 {"projectId": 123456}，后续命令不用每次带 --project
apifox import apifox/openapi.json --project 123456
# 等价写法：apifox import --project 123456 --format openapi --file ./apifox/openapi.json
```

---

## 3. 第三步：配置环境与鉴权

| 配置项 | 值 | 位置 |
|--------|-----|------|
| 环境：本地 | 前置 URL `http://localhost:8080` | 项目设置 → 环境管理 |
| 环境变量 | `token`（登录后写入）、`baseUrl` | 同上 |
| 鉴权方式 | API Key → **in: header**、**name: token**、value `{{token}}` | 项目设置 → 鉴权 / 单接口 Auth |

导入后 `securitySchemes` 会被识别为「API Key / header / token」，在根目录鉴权处填 `{{token}}` 即可全局继承；
`/login` 已在 OpenAPI 里标了 `security: []`，不会带 token。

**登录接口后置脚本**（复制 `apifox/scripts/01-login-extract-token.js`）：

```js
var res = pm.response.json();
if (res.code === 1 && res.data && res.data.token) {
    pm.environment.set("token", res.data.token);
}
```

登录响应结构（`Result.success(LoginInfo)`）：
```json
{ "code": 1, "msg": "success", "data": { "id": 1, "username": "songjiang", "name": "宋江", "token": "eyJhbGciOi..." } }
```

测试账号：`songjiang / 123456`、`lujunyi / 123456`（管理员）。

---

## 4. 第四步：生成测试用例

三种方式，按推荐度排序：

| 方式 | 操作 | 适用场景 |
|------|------|----------|
| AI 生成 | 接口详情页 → 「测试用例」标签 → **通过 AI 生成** → 勾选正向/负向/边界值/安全性 → 生成 → 采纳 | 快速铺量，一次最多 80 条。需 Apifox ≥ 2.7.37 并在设置里启用 AI、配置模型 |
| 从接口一键生成 | 接口 → 「测试用例」→ 新建用例时直接沿用接口参数与示例值 | 冒烟回归、参数已经带示例值的接口 |
| 手动/脚本 | 复制 `apifox/scripts/02-common-assertions.js` 到「后置操作」 | 需要自定义断言与业务校验的场景 |

**建议的最小用例集（每个接口 3 条）**：

1. **正向**：合法参数 → `code=1`，HTTP 200，响应时间达标
2. **参数异常**：缺必填 / 类型错误 / 越界值 → `code=0` 或 400，提示信息明确
3. **鉴权异常**：不带 `token` 头 → 401 或 `code=0`（本项目由 `TokenInterceptor` 拦截，非登录接口必挂）

通用断言片段：

```js
pm.test("业务响应码 code = 1", function () {
    pm.expect(pm.response.json().code).to.eql(1);
});
pm.test("响应时间 < 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});
```

**想批量写用例（不让 AI 瞎猜结构）**：用 CLI 的「取数据模型 → 校验 → 写入」三步：

```bash
apifox cli-schema get test-case-create            # 看测试用例的 JSON 结构
apifox cli-schema validate test-case-create --file ./case.json   # 写入前校验
apifox test-case create --file ./case.json        # 校验通过再写入
```

更安全的是先开 AI 分支（`apifox branch create --type ai`），在分支里导入/编辑，审查后再合并到主分支。

---

## 5. 第五步：编排自动化测试场景

单接口用例跑通后，在 **自动化测试 → 新建场景** 里把接口串成业务链路。
Apifox 支持 If / For / Wait 等流程控制，步骤之间用变量传递数据。

推荐先搭这 4 个场景：

| 场景 | 步骤 | 关键点 |
|------|------|--------|
| **SC01 登录鉴权** | `POST /login` → 提取 `token` → `GET /emps`（带 token）→ `GET /emps`（不带 token） | 验证 token 透传与 401 拦截 |
| **SC02 部门 CRUD** | `POST /depts` 新增 → 提取 `id` → `GET /depts/{id}` 查 → `PUT /depts` 改 → `GET /depts` 校验 → `DELETE /depts?id=` 清理 | 典型增删改查闭环，注意删除接口参数是 query 的 `id` |
| **SC03 学员主流程** | `GET /clazzs/list` 取班级 → `POST /students` 新增学员 → `GET /students?page=1&pageSize=10` → `PUT /students/violation/{id}/{score}` → 删除清理 | 跨模块数据依赖，用环境变量传 ID |
| **SC04 数据一致性** | `GET /report/studentDegreeData` + `GET /report/studentCountData` + `GET /clazzs` 交叉核对人数 | 报表接口无副作用，适合做性能基线（可直接在场景上跑压测） |

**变量传递**：在步骤的「后置操作 → 提取变量」里把响应字段存成环境变量，下一步用 `{{varName}}` 引用；
也可以用脚本 `pm.environment.set("newDeptId", res.data.id)`。

**测试数据清理**：把删除步骤放在场景末尾，并在「场景设置」里勾选「即使前序步骤失败也执行」，避免脏数据堆积。

---

## 6. 第六步：运行与接入 CI

```bash
# 本地跑（从场景的 CI/CD 标签页复制，自带 ID）
apifox run -t <scenarioId> -e <envId> -r cli,html

# 数据驱动：喂 CSV/JSON，逐行循环执行同一场景
apifox run -t <scenarioId> -e <envId> -d ./apifox/testdata.csv

# 报告输出到 ./apifox-reports/，--upload-report 可同步到云端
```

- 断言全过退出码 `0`，有失败为非零 → 流水线可直接卡点，不用额外胶水代码
- 报告格式：`cli` / `html` / `json` / `junit`，可组合使用
- CI 里别用登录态令牌，改成 `--access-token <Secret>` 传入
- 换 `-e` 参数即可让同一套场景指向开发/测试/生产环境

---

## 7. 本项目需要注意的坑

| 项 | 说明 |
|----|------|
| 鉴权头是 `token` | 不是 `Authorization: Bearer`，`TokenInterceptor` 读的是 `token` 头 |
| 限流 | 普通接口 100 次/秒、AI 接口 10 次/秒（`RateLimitInterceptor`），压测场景要注意别把自己限流了 |
| AI 接口 | `POST /ai/chat` 是 SSE 流式（`text/event-stream`），自动化断言建议改用 `POST /ai/chat/sync` |
| AI 接口依赖模型 | 需先在 `config/models.json` 配好可用模型与 Key，否则用例必然失败 |
| 路径参数类型 | `DELETE /depts/{ids}` 是批量删除但走 path；`DELETE /emps?ids=1,2` 走 query，别混 |
| 已知缺陷 | `/report/empGenderData` 后端缺 `/`，见第 1 节 |
| 演示环境拦截器 | `DemoInterceptor` 可能对写操作做限制，批量新增用例前先确认 |

---

## 8. 文件清单

| 文件 | 作用 |
|------|------|
| `tools/export_openapi.py` | **主用**：从运行中的后端 `/v3/api-docs` 导出并增强，自动合并 springdoc 分组 |
| `tools/gen_openapi.py` | 兜底：后端起不来时，从源码静态解析生成同样的文件 |
| `apifox/openapi.json` | 导入 Apifox 的接口定义文件 |
| `apifox/scripts/01-login-extract-token.js` | 登录接口后置脚本：提取 token 写入环境变量 |
| `apifox/scripts/02-common-assertions.js` | 通用断言模板：状态码 / 业务码 / 响应时间 |

接口有变更时，重跑 `python tools/export_openapi.py --wait 120` 再导入一次即可同步（会覆盖同名文件）。
