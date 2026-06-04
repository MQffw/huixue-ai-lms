# 🎓 Tlias 智能教学管理系统

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-18-blue.svg)](https://www.oracle.com/java/)
[![Vue](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg)](https://vuejs.org)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

**基于 Spring Boot + Vue 3 的现代化培训管理系统，集成 AI 智能助手，支持自然语言查询业务数据**

[📖 文档](API接口文档.md) · [🐛 问题反馈](https://github.com/MQffw/Tlias智能教学管理系统/issues)

</div>

---

## ✨ 项目特色

### 🎯 核心功能
- **部门管理** - 企业组织架构管理，支持多级部门结构
- **员工管理** - 员工信息、职位、薪资、工作经历管理
- **班级管理** - 培训课程班级管理，支持学科分类
- **学员管理** - 学员信息、学习进度、违纪记录跟踪
- **数据统计** - 多维度数据可视化统计，支持图表展示
- **文件上传** - 支持图片等资源上传，集成云存储

### 🤖 AI 智能助手
- **自然语言查询** - 使用自然语言查询业务数据，如"我们公司有多少个部门？"
- **智能问答** - 支持上下文对话，记住对话历史
- **数据洞察** - 自动分析业务数据并给出建议
- **流式输出** - SSE 实时推送 AI 回答，提升用户体验

### 🔐 安全特性
- **JWT Token 认证** - 无状态认证方案
- **接口权限控制** - 基于拦截器的权限管理
- **操作日志记录** - 完整的操作审计日志
- **数据验证** - 前后端双重数据验证

### 🎨 用户体验
- **响应式设计** - 完美适配桌面、平板、手机
- **现代化UI** - 基于 Element Plus 的美观界面
- **实时更新** - 数据变化实时反映
- **操作友好** - 简洁直观的操作流程

---

## 📁 项目结构

```
web-ai-project02/
├── 📄 README.md                    # 项目说明文档
├── 📄 API接口文档.md                # 完整API接口文档
├── 📁 tlias-web-management/        # 后端项目
│   ├── 📁 src/                     # 源代码
│   │   ├── 📁 main/
│   │   │   ├── 📁 java/            # Java源码
│   │   │   ├── 📁 resources/       # 配置文件
│   │   │   └── 📁 mapper/          # MyBatis映射
│   │   └── 📁 test/                # 测试代码
│   ├── 📁 front-dist/              # 前端构建产物
│   │   └── 📁 dist/
│   │       ├── 📄 index.html        # 前端入口
│   │       └── 📁 assets/           # 静态资源
│   ├── 📄 pom.xml                   # Maven配置
│   └── 📁 target/                  # 构建输出
└── 📁 logs/                        # 日志文件
```

---

## 🛠️ 技术栈

### 后端技术
<div align="center">

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 18 | 编程语言 |
| Spring Boot | 3.5.13 | 应用框架 |
| MyBatis | 3.5.3 | ORM框架 |
| MySQL | 8.0 | 数据库 |
| JWT | - | 认证方案 |
| LongCat AI | - | 美团大模型 |

</div>

### 前端技术
<div align="center">

| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式JavaScript框架 |
| Vite | 极速构建工具 |
| Element Plus | Vue 3 UI组件库 |
| Axios | Promise HTTP客户端 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |

</div>

---

## 🚀 快速开始

### 环境要求

- JDK 18+
- Maven 3.6+
- MySQL 8.0+
- LongCat API Key（环境变量 `longcat-api`）

### 1. 克隆项目

```bash
git clone https://github.com/MQffw/Tlias智能教学管理系统.git
cd Tlias智能教学管理系统
```

### 2. 数据库配置

```sql
-- 创建数据库
CREATE DATABASE tlias DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `tlias-web-management/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tlias?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

# LongCat AI 配置
longcat-api: your-api-key-here
```

### 3. 启动项目

**方式一：使用 Maven 直接运行**
```bash
cd tlias-web-management
mvn spring-boot:run
```

**方式二：打包后运行**
```bash
cd tlias-web-management
mvn clean package -DskipTests
java -jar target/tlias-web-management-0.0.1-SNAPSHOT.jar
```

### 4. 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端API | `http://localhost:8080` | RESTful API 接口 |
| 前端界面 | `http://localhost:8080` | 自动加载前端静态资源 |

### 5. 测试账号

| 用户名 | 密码 | 权限 |
|--------|------|------|
| jinyong | 123456 | 管理员 |
| songjiang | 123456 | 管理员 |
| shinaian | 123456 | 管理员 |

### 4. 访问系统

- 🌐 **前端地址**：http://localhost:8080
- 📡 **后端API**：http://localhost:8080
- 🤖 **AI助手**：内置于系统中

---

## 🎯 功能模块

### 1. 🤖 AI 智能助手
基于 LongCat 大模型的智能助手，支持：
- 通用聊天对话
- 业务数据查询（部门、员工、班级、学员统计）
- 上下文理解

**测试账号**：
```
用户名：shinaian / songjiang / lujunyi
密码：123456
```

### 2. 🏢 部门管理
- 部门列表查询
- 部门增删改查
- 部门信息维护

### 3. 👥 员工管理
- 员工信息分页查询
- 员工档案管理
- 工作经历记录
- 职位信息管理

### 4. 🏫 班级管理
- 班级信息维护
- 班主任分配
- 学科管理
- 开班状态跟踪

### 5. 👨‍🎓 学员管理
- 学员档案管理
- 班级分配
- 学历信息统计
- 违纪记录处理

### 6. 📊 数据统计
- 员工性别统计
- 职位分布统计
- 学员学历统计
- 班级人数统计
- 操作日志查询

---

## 📋 API 接口概览

### 通用响应格式

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

### 认证说明

登录后获取 JWT 令牌，后续请求需在 Header 中携带：

```
token: eyJhbGciOiJIUzI1NiJ9...
```

未认证请求返回 `401` 状态码。

### 主要接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | `/login` | 用户登录 |
| AI助手 | POST | `/ai/chat` | 流式聊天 |
| 部门 | GET | `/depts` | 部门列表 |
| 员工 | GET | `/emps` | 员工列表 |
| 班级 | GET | `/clazzs` | 班级列表 |
| 学员 | GET | `/students` | 学员列表 |
| 统计 | GET | `/report/*` | 数据统计 |

📖 **详细API文档**：[API接口文档.md](API接口文档.md)

---

## 📸 系统截图

> *（此处可添加系统界面截图）*

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📄 许可证

本项目基于 [MIT](LICENSE) 许可证开源。

---

## 👨‍💻 作者

**MQffw** - [GitHub](https://github.com/MQffw)

---

## 🤝 贡献指南

我们欢迎各种形式的贡献！如果你想为这个项目做出贡献，请：

1. **Fork** 这个项目
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 **Pull Request**

### 贡献者
感谢所有为这个项目做出贡献的开发者！

---

## 📋 许可证

本项目基于 [MIT License](LICENSE) 开源，你可以自由使用、修改和分发。

```
MIT License

Copyright (c) 2024 Tlias Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## ⭐ 支持项目

如果这个项目对你有帮助，请给个 Star ⭐！你的支持是我们持续改进的动力！

[![Star History Chart](https://api.star-history.com/svg?repos=MQffw/Tlias智能教学管理系统&type=Date)](https://star-history.com/#MQffw/Tlias智能教学管理系统&Date)

---

## 📞 联系方式

如有问题或建议，欢迎：
- 提交 [Issue](../../issues)
- 发起 [Pull Request](../../pulls)
- 联系作者：[MQffw](https://github.com/MQffw)

---

<div align="center">

**🎉 感谢使用 Tlias 智能教学管理系统！**

[⬆ 回到顶部](#-tlias-智能教学管理系统)

---

<div align="center">

Made with ❤️ by Tlias Team

</div>

</div>
