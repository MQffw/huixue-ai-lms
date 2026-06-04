# tlias 智能学习辅助系统

基于 Spring Boot 的培训管理系统，提供部门、员工、班级、学员等管理功能，集成 AI 智能助手支持自然语言查询业务数据。

## 技术栈

### 后端技术
- Java 18
- Spring Boot 3.5.13
- MyBatis
- MySQL
- JWT 认证
- LongCat AI（美团大模型）

### 前端技术
- Vue 3 + Vite
- Element Plus UI
- 前端构建产物位于 `tlias-web-management/front-dist/dist/`

## 快速启动

### 环境要求

- JDK 18+
- Maven 3.6+
- MySQL 8.0+
- LongCat API Key（环境变量 `longcat-api`）

### 数据库准备

```sql
CREATE DATABASE tlias DEFAULT CHARACTER SET utf8mb4;
```

修改数据库配置 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tlias
    username: root
    password: 123456
```

### 启动应用

```bash
cd tlias-web-management
mvn spring-boot:run
```

或打包后运行：

```bash
mvn package
java -jar target/tlias-web-management-0.0.1-SNAPSHOT.jar
```

### 访问地址

- 服务地址：`http://localhost:8080`
- 前端地址：`http://localhost:8080`（自动加载前端静态资源）

---

## 前端资源说明

### front-dist 目录结构

```
tlias-web-management/front-dist/
└── dist/
    ├── index.html          # 前端入口文件
    ├── favicon.ico         # 网站图标
    └── assets/             # 静态资源目录
        ├── index.70e40127.js  # 打包后的JavaScript
        └── index.99f0b67d.css # 打包后的CSS样式
```

### 前端特性

- **现代化UI**：基于 Vue 3 + Element Plus 构建响应式界面
- **单页应用**：使用 Vite 构建，提供快速的开发和构建体验
- **自动部署**：后端服务启动时自动加载前端静态资源
- **API集成**：前端自动调用后端API接口，实现完整的业务功能

### 开发说明

前端源码不包含在本仓库中，如需前端源码请单独获取。当前仓库只包含构建后的生产版本，位于 `front-dist/dist/` 目录下。

---

## API 接口文档

### 通用响应格式

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | number | 响应码，1 成功，0 失败 |
| msg | string | 提示信息 |
| data | object | 返回数据 |

### 认证说明

登录后获取 JWT 令牌，后续请求需在 Header 中携带：

```
token: eyJhbGciOiJIUzI1NiJ9...
```

未认证请求返回 `401` 状态码。

---

### 1. AI 智能助手

基于 LongCat 大模型的 AI 助手，支持通用聊天和业务数据问答。

#### 1.1 流式聊天（SSE）

**POST** `/ai/chat`

请求体：

```json
{
  "message": "我们公司有多少个部门？",
  "history": [
    {"role": "user", "content": "你好"},
    {"role": "assistant", "content": "你好！有什么可以帮助你的？"}
  ]
}
```

响应：SSE 流式返回

```
data: 根据

data: 数据库查询

data: 结果，

data: 当前系统共有

data: 5个部门。
```

#### 1.2 同步聊天

**POST** `/ai/chat/sync`

请求体同上，响应：

```json
{
  "code": 1,
  "msg": "success",
  "data": "根据数据库查询结果，当前系统共有 **5** 个部门。"
}
```

#### 功能特性

- **通用聊天**：回答各类问题，支持上下文对话
- **数据问答**：自动查询数据库回答业务问题，如部门数量、员工信息、班级学员统计等
- **流式输出**：SSE 实时推送 AI 回答，提升用户体验

#### 测试账号

```
用户名：shinaian / songjiang / lujunyi
密码：123456
```

---

### 2. 登录

**POST** `/login`

请求体：

```json
{
  "username": "jinyong",
  "password": "123456"
}
```

响应：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 2,
    "username": "songjiang",
    "name": "宋江",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

### 3. 部门管理

#### 3.1 查询部门列表

**GET** `/depts`

响应示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "学工部",
      "createTime": "2022-09-01T23:06:29",
      "updateTime": "2022-09-01T23:06:29"
    }
  ]
}
```

#### 3.2 根据 ID 查询部门

**GET** `/depts/{id}`

#### 3.3 添加部门

**POST** `/depts`

```json
{
  "name": "教研部"
}
```

#### 3.4 修改部门

**PUT** `/depts`

```json
{
  "id": 1,
  "name": "教研部"
}
```

#### 3.5 删除部门

**DELETE** `/depts?id=1`

---

### 4. 员工管理

#### 4.1 员工列表查询（分页）

**GET** `/emps`

| 参数 | 必须 | 说明 |
|------|------|------|
| name | 否 | 姓名 |
| gender | 否 | 性别（1 男，2 女） |
| begin | 否 | 入职开始日期 |
| end | 否 | 入职结束日期 |
| page | 是 | 页码，默认 1 |
| pageSize | 是 | 每页记录数，默认 10 |

响应示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 2,
    "rows": [
      {
        "id": 1,
        "username": "jinyong",
        "name": "金庸",
        "gender": 1,
        "job": 2,
        "salary": 8000,
        "entryDate": "2015-01-01",
        "deptId": 2,
        "deptName": "教研部"
      }
    ]
  }
}
```

#### 4.2 查询全部员工

**GET** `/emps/list`

#### 4.3 根据 ID 查询员工

**GET** `/emps/{id}`

#### 4.4 添加员工

**POST** `/emps`

```json
{
  "username": "linpingzhi",
  "name": "林平之",
  "gender": 1,
  "job": 1,
  "entryDate": "2022-09-18",
  "deptId": 1,
  "salary": 8000,
  "exprList": [
    {
      "company": "百度科技股份有限公司",
      "job": "java开发",
      "begin": "2012-07-01",
      "end": "2019-03-03"
    }
  ]
}
```

#### 4.5 修改员工

**PUT** `/emps`

#### 4.6 删除员工

**DELETE** `/emps?ids=1,2,3`

---

### 5. 班级管理

#### 5.1 班级列表查询（分页）

**GET** `/clazzs`

| 参数 | 必须 | 说明 |
|------|------|------|
| name | 否 | 班级名称 |
| begin | 否 | 结课开始日期 |
| end | 否 | 结课结束日期 |
| page | 是 | 页码，默认 1 |
| pageSize | 是 | 每页记录数，默认 10 |

响应示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 6,
    "rows": [
      {
        "id": 7,
        "name": "黄埔四期",
        "room": "209",
        "beginDate": "2023-08-01",
        "endDate": "2024-02-15",
        "masterId": 7,
        "masterName": "纪晓芙",
        "status": "已开班"
      }
    ]
  }
}
```

#### 5.2 查询所有班级

**GET** `/clazzs/list`

#### 5.3 根据 ID 查询班级

**GET** `/clazzs/{id}`

#### 5.4 添加班级

**POST** `/clazzs`

```json
{
  "name": "JavaEE就业166期",
  "room": "101",
  "beginDate": "2023-06-01",
  "endDate": "2024-01-25",
  "masterId": 7,
  "subject": 1
}
```

> subject: 1-java, 2-前端, 3-大数据, 4-Python, 5-Go, 6-嵌入式

#### 5.5 修改班级

**PUT** `/clazzs`

#### 5.6 删除班级

**DELETE** `/clazzs/{id}`

---

### 6. 学员管理

#### 6.1 学员列表查询（分页）

**GET** `/students`

| 参数 | 必须 | 说明 |
|------|------|------|
| name | 否 | 学员姓名 |
| degree | 否 | 学历（1 初中, 2 高中, 3 大专, 4 本科, 5 硕士, 6 博士） |
| clazzId | 否 | 班级 ID |
| page | 是 | 页码，默认 1 |
| pageSize | 是 | 每页记录数，默认 10 |

响应示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 5,
    "rows": [
      {
        "id": 3,
        "name": "Lily",
        "no": "2023001003",
        "gender": 2,
        "phone": "13309230912",
        "degree": 4,
        "clazzId": 1,
        "clazzName": "黄埔班一期",
        "violationCount": 2,
        "violationScore": 5
      }
    ]
  }
}
```

#### 6.2 根据 ID 查询学员

**GET** `/students/{id}`

#### 6.3 添加学员

**POST** `/students`

```json
{
  "name": "阿大",
  "no": "2024010801",
  "gender": 1,
  "phone": "15909091235",
  "degree": 4,
  "clazzId": 9
}
```

#### 6.4 修改学员

**PUT** `/students`

#### 6.5 删除学员

**DELETE** `/students/{ids}`

> ids 为逗号分隔的 ID，如 `/students/1,2,3`

#### 6.6 违纪处理

**PUT** `/students/violation/{id}/{score}`

> 扣除学员 `id` 的 `score` 分

---

### 7. 文件上传

**POST** `/upload`

- Content-Type: `multipart/form-data`
- 参数：`file`（最大 10MB）

响应：

```json
{
  "code": 1,
  "msg": "success",
  "data": "https://xxx.jpg"
}
```

---

### 8. 数据统计

#### 8.1 员工性别统计

**GET** `/report/empGenderData`

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {"name": "男性员工", "value": 5},
    {"name": "女性员工", "value": 6}
  ]
}
```

#### 8.2 员工职位统计

**GET** `/report/empJobData`

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "jobList": ["教研主管","学工主管","其他","班主任","咨询师","讲师"],
    "dataList": [1,1,2,6,8,13]
  }
}
```

#### 8.3 学员学历统计

**GET** `/report/studentDegreeData`

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {"name": "本科", "value": 182},
    {"name": "大专", "value": 126}
  ]
}
```

#### 8.4 班级人数统计

**GET** `/report/studentCountData`

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "clazzList": ["Java就业100期","Java就业101期"],
    "dataList": [77,82]
  }
}
```

#### 8.5 日志查询

**GET** `/log/page`

| 参数 | 必须 | 说明 |
|------|------|------|
| page | 是 | 页码，默认 1 |
| pageSize | 是 | 每页记录数，默认 10 |
