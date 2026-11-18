# 轻量在线判题系统（LightWeight Online Problem Solving）

一个面向课程场景的轻量在线判题（OJ）系统：学生在线读题、编写代码、提交判题，实时查看每个测试点的执行结果；管理员维护题目与测试数据。另附可整体下线的 AI 助教模块，只在提交失败后给出**启发式提示**，不直接给答案或可提交代码。

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)

## 功能一览

| 模块 | 说明 |
|---|---|
| 认证 | JWT 无状态登录，角色分为 `STUDENT` / `ADMIN` |
| 题库 | 6 道示例题（难度 1–2，每题 6 个隐藏测试点）；管理员可增删改与设置可见性 |
| 用户管理 | 管理员查看账号列表、启用/禁用、重置密码；禁用后已登录会话立即失效 |
| 个人中心 | 修改昵称/密码、上传头像；今日刷题统计与近 7 天趋势；分页刷题记录 |
| 判题 | 提交后异步判题，编译 + 逐测试点执行，支持 Python 3 / C++ 17 / Java 17 |
| 实时反馈 | WebSocket（STOMP）推送提交状态变化，无需手动刷新 |
| 提交记录 | 历史列表、单次提交的每个测试点耗时与结论（AC / WA / TLE / MLE / RE / CE） |
| 排行榜 | 按通过题数、罚时统计 |
| AI 助教 | 失败后调用 Agent，通过受控工具读取脱敏信息，分步给出思路提示（可一键关闭） |

## 技术栈

| 层次 | 选型 |
|---|---|
| 前端 | Vue 3.5 + Vue Router + Pinia + Vite 5 + Monaco Editor + Markdown-It |
| 后端 | Spring Boot 3.3（Web / Security / Validation / Actuator）+ MyBatis-Plus 3.5 |
| 鉴权 | Spring Security + JJWT 0.12 |
| 数据库 | MySQL 8.0（InnoDB / utf8mb4），6 张表 |
| 接口文档 | SpringDoc OpenAPI 2.5（Swagger UI） |
| 测试 | JUnit 5 + JaCoCo |

## 项目结构

```
backend/                Spring Boot 后端
  ├─ config/            安全、线程池、判题与 AI 配置
  ├─ controller/        REST 接口
  ├─ service/           业务逻辑
  ├─ judge/             判题调度、引擎、输出比对
  │   └─ sandbox/       进程沙箱执行器
  ├─ ai/                Agent 编排、LLM 客户端、受控工具
  ├─ websocket/         结果推送
  └─ mapper/            MyBatis-Plus Mapper
frontend/               Vue 3 前端（Vite）
docs/                   接口契约、详细设计说明书、数据库脚本
scripts/                数据库初始化、端到端冒烟测试脚本
```

## 快速开始

**前置环境**：JDK 17、Maven 3.9+、Node 18+、pnpm、MySQL 8.0（字符集 utf8mb4）。

### 1. 初始化数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < docs/sql/schema.sql
```

Windows 也可以使用脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/init-db.ps1 -Password 你的MySQL密码
```

脚本会创建 `oj` 库、6 张表，并写入 6 道示例题共 36 个隐藏测试点（每题 6 个，公开样例不参与评分）：A+B、数列求和、求最大值、回文串判断、斐波那契数列、排序去重。脚本使用 `INSERT ... WHERE NOT EXISTS`，可重复执行而不会产生重复数据。测试点由 `scripts/gen_test_cases.py` 以固定种子生成，重跑结果一致。

### 2. 启动后端

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `OJ_DB_URL` | `jdbc:mysql://localhost:3306/oj...` | 数据源地址 |
| `OJ_DB_USER` / `OJ_DB_PASSWORD` | `root` / `root` | 数据库账号 |
| `SERVER_PORT` | `8081` | 后端监听端口，前端默认直连该端口 |
| `OJ_JWT_SECRET` | 开发占位值 | 部署必须替换为 ≥32 字节的随机串 |
| `OJ_ADMIN_PASSWORD` | 空 | **非空时**启动会把 `admin` 的密码重置为该值 |
| `OJ_SANDBOX_TYPE` | `process` | `process` 为本地子进程沙箱 |
| `OJ_JUDGE_CONCURRENCY` | `4` | 判题并发线程数 |
| `OJ_AI_ENABLED` | `false` | AI 助教总开关 |

```bash
# Windows CMD（默认监听 8081，无需再设 SERVER_PORT）
set OJ_ADMIN_PASSWORD=Admin@123
mvn -f backend/pom.xml spring-boot:run
```

```powershell
# PowerShell
$env:OJ_ADMIN_PASSWORD="Admin@123"
mvn -f backend/pom.xml spring-boot:run
```

启动后接口文档位于 http://localhost:8081/swagger-ui.html 。

### 3. 启动前端

```bash
pnpm -C frontend install
pnpm -C frontend dev
```

访问 http://localhost:5173 。前端在 `frontend/src/api/config.js` 中直连 `http://<当前主机>:8081`，不经过 Vite 代理；后端地址变化时，用 `VITE_API_BASE` / `VITE_WS_BASE` 覆盖即可。

### 4. 登录

| 账号 | 密码 | 说明 |
|---|---|---|
| `admin` | 启动时设置的 `OJ_ADMIN_PASSWORD` | 管理员 |
| `stu001` | — | 库中为占位哈希，无法直接登录；由管理员在「用户管理」中重置密码 |

> 若启动时未设置 `OJ_ADMIN_PASSWORD`，`admin` 无法登录（库中只有占位哈希）。

管理员登录后可在侧边栏「用户管理」中：

- 查看全部账号、角色与状态
- 新增学生账号（用户名唯一，昵称与密码可选）
- 启用 / 禁用账号；禁用后该账号已登录的会话立即失效
- 重置密码，重置值由 `OJ_ADMIN_RESET_PASSWORD` 决定，默认 `12345678`

## 判题说明

提交流程：`提交 → 幂等检查 → 入队 → 编译 → 逐测试点执行 → 结果落库 → WebSocket 推送`。

- 幂等：同一用户对同一题目的相同代码在 10 分钟窗口内复用既有结果，避免重复判题。
- 背压：队列容量 200，满时直接拒绝而非无限堆积；判题并发数可配置。
- 时间限制：以题目 `time_limit_ms` 为基准，按语言乘以系数（Python 3 为 ×3、C++17 为 ×1、Java 17 为 ×2）。
- 编译超时 20 秒，编译输出截断上限 8 KB。
- fail-fast：遇到第一个未通过的测试点即停止后续评测，只累计已通过点的分数；因此硬编码打表最多骗到首个点，无法拿满分。
- 防打表：公开样例不进入评分数据；评分点包含边界、负数、极值与 1 万规模随机数据，且输入与期望输出不会通过任何学生接口返回。数据由 `scripts/gen_test_cases.py` 生成。

## AI 助教（可选）

默认关闭，关闭时判题主链路完全不受影响。**凭据按用户配置**：每个账号在自己的「个人设置」页填写端点、模型与 API Key；未配置的账号看不到助教入口，也不会产生任何模型调用。

### 个人配置步骤

1. 登录后打开侧边栏「个人设置」
2. 填写：
   - 模型端点：OpenAI 兼容地址，写到 `/v1` 为止（程序会自动拼接 `/chat/completions`）
   - 模型名称：如 `qwen-plus`，必须支持 function calling
   - API Key：加密存储，页面只显示 `sk-****1234`
3. 点「测试连接」，返回 `连接成功` 即配置可用
4. 保存后打开任意非 AC 提交即可使用

### 接入阿里云百炼

| 项 | 值 |
|---|---|
| 端点 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| 推荐模型 | `qwen-plus`（支持工具调用）；也可用 `qwen-max` / `qwen-turbo` |

专属 MaaS 端点（形如 `https://llm-xxxx.cn-beijing.maas.aliyuncs.com/compatible-mode/v1`）同样适用，只替换端点即可。海外账号使用 `dashscope-intl.aliyuncs.com`。

### 安全与运维

- API Key 使用 AES-GCM 加密后存入 `user_ai_config`，加密密钥来自环境变量 `OJ_SECRET_KEY`（默认值为开发占位，部署必须替换）
- 管理员可在「用户管理 → AI 配置」查看某人是否已配置、清空其配置，**看不到明文 Key**
- 用户未配置或停用时，`/api/ai/status` 返回 `enabled=false`，前端不渲染入口并在顶部提示「去配置」
- 全局开关 `OJ_AI_ENABLED` 仍是总闸，关闭后所有用户都无法使用

### 本地配置覆盖

Spring Boot 会读取工作目录下的 `config/application.properties`（已加入 `.gitignore`），常用于存放本机数据库密码：

```properties
spring.datasource.password=你的数据库密码
```

AI 凭据不要写在这里，请到「个人设置」页填写。

- 分级提示：思路 → 定位 → 检查点，首次只给方向，可点「再具体一点」逐层深入。
- 可追问：内置常用快捷问题，也可输入自定义问题；同一提交的历史问答会持久化并回看。
- 结果复用：同一提交 + 同一问题 + 同一层级直接回放已存回答，不重复消耗额度。
- Agent 只能通过受控工具读取信息，且拿到的是**脱敏摘要**（失败点序号、错误类型、输出形状），不含测试数据内容。
- 限流：每用户每日 100 次、全局每日 500 次；最小间隔默认关闭（`OJ_AI_MIN_INTERVAL_SECONDS` 设为正数即可开启冷却）；最多 3 步推理。
- 已 AC 的提交不提供助教；未配置或停用的用户不渲染入口。
- API Key 加密存储于数据库，仓库内没有任何真实密钥。

## 测试

```bash
mvn -f backend/pom.xml test
```

端到端冒烟（需后端已启动、`admin` 密码为 `Admin@123`、存在 ID 为 1 的题目）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/e2e-test.ps1 -BaseUrl http://127.0.0.1:8081
```

脚本依次完成登录、提交 A+B 的 Python 解法、轮询并打印各测试点结果。

## 容器化

`backend/Dockerfile` 提供多阶段构建（Maven 构建 → JRE 运行，暴露 8081）。根目录 `docker-compose.json` 是 MySQL + 后端的编排草稿，重命名为 `docker-compose.yml` 后方可使用；前端未包含在编排内，需单独构建或本地启动。

## 文档

- [接口契约](docs/接口契约.md) — 前后端并行开发的唯一依据
- [详细设计说明书](docs/详细设计说明书.md) — 模块划分、状态机、异常与测试切入点
- [数据库脚本](docs/sql/schema.sql) — 建库建表与初始数据

## 已知限制

- 仅支持 3 种语言，未实现 Special Judge 与浮点误差比对。
- 单机部署，无分布式调度与多节点扩展。
- 进程沙箱仅做超时控制与工作目录隔离，未限制内存与网络，适合课程与内网场景，不建议直接暴露到公网。