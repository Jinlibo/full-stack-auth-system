# 全栈认证授权系统 - 部署与使用文档

## 一、项目概述

本系统由两个完整的前后端项目组成：

| 项目                | 说明                  | 后端端口 | 前端端口 |
|-------------------|---------------------|------|------|
| **auth-platform** | 认证授权平台（OAuth2授权服务器） | 8080 | 5173 |
| **product-app**   | 产品应用（OAuth2客户端）     | 8081 | 5174 |

### 核心功能

**Auth Platform（认证平台）**

- 用户管理：注册、登录、CRUD、个人中心
- RBAC权限管理：角色管理、权限管理、角色权限分配
- 产品管理：产品CRUD、自动注册为OAuth2客户端
- OAuth2授权服务器：基于Spring Authorization Server

**Product App（产品应用）**

- 多方式登录：账号密码登录 + OAuth2第三方登录
- 用户统一映射：不同登录方式映射到同一用户记录
- 用户管理 + RBAC权限控制

## 二、技术栈

**后端**：Java 17, Spring Boot 3.2, Spring Security, Spring Authorization Server, MyBatis-Plus, MySQL 8.0, Redis, JWT

**前端**：Vue 3, Vite 5, Vue Router 4, Pinia, Element Plus, Axios

## 三、环境准备

### 3.1 必需环境

| 组件      | 版本要求 |
|---------|------|
| JDK     | 17+  |
| Maven   | 3.8+ |
| Node.js | 18+  |
| MySQL   | 8.0+ |
| Redis   | 6.0+ |

### 3.2 数据库初始化

```bash
# 执行SQL脚本（在项目根目录sql文件夹下）
mysql -u root -p < sql/auth_platform.sql
mysql -u root -p < sql/product_app.sql
```

### 3.3 配置修改

根据你的实际环境修改以下配置文件：

**auth-platform/backend/src/main/resources/application.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_platform  # 修改数据库地址
    username: root                                    # 修改用户名
    password: root123                                 # 修改密码
  data:
    redis:
      host: localhost    # 修改Redis地址
      port: 6379
      password:          # 如有密码请填写
```

**product-app/backend/src/main/resources/application.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/product_app
    username: root
    password: root123
  data:
    redis:
      host: localhost
      port: 6379
      database: 1        # 使用不同的db避免key冲突

oauth2:
  client:
    client-secret: admin123  # 与SQL初始化数据中的密码一致
```

## 四、启动步骤

### 4.1 启动后端

```bash
# 1. 启动Auth Platform后端
cd auth-platform/backend
mvn clean spring-boot:run

# 2. 启动Product App后端（另开终端）
cd product-app/backend
mvn clean spring-boot:run
```

### 4.2 启动前端

```bash
# 3. 启动Auth Platform前端（另开终端）
cd auth-platform/frontend
npm install
npm run dev
# 访问 http://localhost:5173

# 4. 启动Product App前端（另开终端）
cd product-app/frontend
npm install
npm run dev
# 访问 http://localhost:5174
```

## 五、使用流程

### 5.1 管理员登录认证平台

1. 打开 http://localhost:5173
2. 使用默认账号登录：`admin` / `admin123`
3. 可进行用户管理、角色管理、权限管理、产品管理

### 5.2 产品应用 - 账号密码登录

1. 打开 http://localhost:5174
2. 选择"账号密码登录"
3. 使用默认账号：`admin` / `admin123`

### 5.3 产品应用 - OAuth2第三方登录（核心流程）

1. 打开 http://localhost:5174
2. 选择"第三方登录" → 点击"认证平台授权登录"
3. 跳转到认证平台登录页面（http://localhost:8080/login）
4. 输入认证平台账号（如 admin/admin123）
5. 同意授权后自动回调到产品应用
6. 系统自动完成：
    - 用授权码换取access_token
    - 获取认证平台用户信息
    - 创建/关联本地用户
    - 生成本地JWT token
    - 登录成功

### 5.4 OAuth2登录流程图

```
产品应用前端 → 后端获取授权URL → 重定向到认证平台
    ↓
认证平台: 用户登录 → 授权同意页
    ↓
回调到产品应用: /oauth/callback?code=xxx&state=xxx
    ↓
产品应用后端:
  1. POST /oauth2/token (用code换token)
  2. GET /api/oauth2/userinfo (获取用户信息)
  3. 查找/创建本地用户 + OAuth绑定
  4. 生成本地JWT返回前端
    ↓
前端保存JWT → 登录完成
```

## 六、项目结构

```
projects/
├── sql/
│   ├── auth_platform.sql       # 认证平台数据库脚本
│   └── product_app.sql         # 产品应用数据库脚本
├── auth-platform/
│   ├── backend/
│   │   ├── pom.xml
│   │   └── src/main/java/com/auth/platform/
│   │       ├── AuthPlatformApplication.java
│   │       ├── common/          # R, BusinessException, GlobalExceptionHandler
│   │       ├── config/          # SecurityConfig, AuthorizationServerConfig, CorsConfig, RedisConfig
│   │       ├── controller/      # AuthController, SysUser/Role/Permission/ProductController, OAuth2UserInfoController
│   │       ├── dto/             # 请求/响应DTO
│   │       ├── entity/          # MyBatis-Plus实体
│   │       ├── mapper/          # Mapper接口
│   │       ├── security/        # JWT工具, 认证过滤器, UserDetailsService
│   │       └── service/         # 业务层接口与实现
│   └── frontend/
│       ├── src/
│       │   ├── api/             # API接口封装
│       │   ├── components/      # Layout布局
│       │   ├── router/          # 路由配置
│       │   ├── store/           # Pinia状态管理
│       │   ├── utils/           # Axios封装
│       │   └── views/           # 页面组件
│       └── package.json
└── product-app/
    ├── backend/                  # 结构同上
    └── frontend/                 # 结构同上（含OAuth回调页）
```

## 七、API接口文档

### Auth Platform (端口8080)

| 方法     | 路径                    | 说明           | 认证                 |
|--------|-----------------------|--------------|--------------------|
| POST   | /api/auth/login       | 登录           | 无                  |
| POST   | /api/auth/register    | 注册           | 无                  |
| POST   | /api/auth/refresh     | 刷新Token      | 无                  |
| POST   | /api/auth/logout      | 退出           | 需要                 |
| GET    | /api/users            | 用户分页列表       | system:user:query  |
| GET    | /api/users/me         | 当前用户信息       | 需要                 |
| POST   | /api/users            | 创建用户         | system:user:add    |
| PUT    | /api/users/{id}       | 更新用户         | system:user:edit   |
| DELETE | /api/users/{id}       | 删除用户         | system:user:delete |
| GET    | /api/roles            | 角色列表         | system:role:query  |
| GET    | /api/roles/all        | 全部角色         | 需要                 |
| POST   | /api/roles            | 创建角色         | system:role:add    |
| PUT    | /api/roles/{id}       | 更新角色         | system:role:edit   |
| DELETE | /api/roles/{id}       | 删除角色         | system:role:delete |
| GET    | /api/permissions/tree | 权限树          | 需要                 |
| POST   | /api/permissions      | 创建权限         | system:permission  |
| GET    | /api/products         | 产品列表         | product:list       |
| POST   | /api/products         | 创建产品         | product:add        |
| GET    | /api/oauth2/userinfo  | OAuth2用户信息端点 | Bearer Token       |
| GET    | /oauth2/authorize     | OAuth2授权端点   | 无                  |
| POST   | /oauth2/token         | OAuth2令牌端点   | 无                  |

### Product App (端口8081)

| 方法   | 路径                             | 说明            | 认证        |
|------|--------------------------------|---------------|-----------|
| POST | /api/auth/login                | 账号密码登录        | 无         |
| GET  | /api/auth/oauth2/authorize-url | 获取OAuth2授权URL | 无         |
| POST | /api/auth/oauth2/callback      | OAuth2回调处理    | 无         |
| POST | /api/auth/logout               | 退出            | 需要        |
| GET  | /api/users/me                  | 当前用户信息        | 需要        |
| PUT  | /api/users/me/profile          | 更新个人信息        | 需要        |
| GET  | /api/users                     | 用户列表          | user:list |

## 八、默认账号

| 系统   | 用户名   | 密码       | 角色    |
|------|-------|----------|-------|
| 认证平台 | admin | admin123 | 超级管理员 |
| 产品应用 | admin | admin123 | 管理员   |

## 九、注意事项

1. **OAuth2客户端密钥**：SQL初始化数据中 `product-app` 的 `client_secret` 使用了BCrypt加密的 `admin123`，Product App配置文件中的
   `client-secret` 需要与之匹配（填写原文 `admin123`）

2. **Redis数据库隔离**：两个项目使用不同的Redis数据库（db0和db1），避免key冲突

3. **CORS配置**：已预配置跨域允许，生产环境请修改为实际域名

4. **RSA密钥**：当前Authorization Server每次启动会重新生成RSA密钥对，生产环境应持久化存储

5. **前端代理**：开发环境通过Vite proxy转发API请求，生产环境需配置Nginx

## 十、生产环境部署建议

```nginx
# Nginx配置示例
server {
    listen 80;
    server_name auth.example.com;
    
    location / {
        root /path/to/auth-platform/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080;
    }
    
    location /oauth2/ {
        proxy_pass http://localhost:8080;
    }
}

server {
    listen 80;
    server_name app.example.com;
    
    location / {
        root /path/to/product-app/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8081;
    }
}
```
