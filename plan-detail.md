1. 一、项目总体说明

   

   本系统由两个独立的前后端分离项目组成：

   

   Auth Center（统一认证中心）

   

   Product System（具体业务系统）

   

   两个系统通过 OAuth2 授权协议实现单点登录与账号绑定。

   

   二、系统总体目标

   

   系统需要实现以下能力：

   

   提供统一的用户管理系统

   

   提供 RBAC 权限控制

   

   提供 OAuth2 授权服务器

   

   支持多个业务系统接入

   

   支持 OAuth 登录

   

   支持账号绑定与解绑

   

   支持本地账号密码登录

   

   前后端完全分离

   

   使用 REST API 通信

   

   三、系统架构

   

   系统结构如下：

   

   用户浏览器

   ​     │

   ​     │

   ​     ├──── Auth Center（认证中心）

   ​     │          │

   ​     │          ├ 用户管理

   ​     │          ├ 权限管理

   ​     │          ├ OAuth2 授权

   ​     │          └ Client管理

   ​     │

   ​     │

   ​     └──── Product System（业务系统）

   ​                │

   ​                ├ 用户管理

   ​                ├ 权限管理

   ​                ├ OAuth登录

   ​                └ OAuth账号绑定

   四、Auth Center 功能需求

   

   Auth Center 是系统的核心认证平台。

   

   需要实现以下模块。

   

   4.1 用户管理模块

   

   功能包括：

   

   用户注册

   

   允许用户创建账号。

   

   用户需要提供：

   

   用户名

   

   密码

   

   邮箱（可选）

   

   手机号（可选）

   

   系统需要完成：

   

   校验用户名唯一性

   

   创建用户账号

   

   返回注册成功信息

   

   用户登录

   

   用户使用账号密码登录。

   

   系统需要：

   

   校验用户名密码

   

   登录成功后生成访问令牌

   

   返回访问令牌

   

   用户信息查询

   

   用户可以查询自己的信息。

   

   系统需要返回：

   

   用户ID

   

   用户名

   

   邮箱

   

   手机号

   

   状态

   

   用户信息修改

   

   用户可以修改：

   

   邮箱

   

   手机号

   

   昵称

   

   密码

   

   用户注销

   

   用户可以注销账号。

   

   注销后账号状态需要被标记为：

   

   disabled

   4.2 权限管理模块（RBAC）

   

   系统需要实现基于角色的访问控制。

   

   RBAC模型包含：

   

   User

   Role

   Permission

   

   系统功能包括：

   

   角色管理

   

   管理员可以：

   

   创建角色

   

   修改角色

   

   删除角色

   

   查询角色列表

   

   权限管理

   

   管理员可以：

   

   创建权限

   

   修改权限

   

   删除权限

   

   查询权限列表

   

   用户角色分配

   

   管理员可以：

   

   给用户分配角色

   

   移除用户角色

   

   查询用户角色

   

   角色权限分配

   

   管理员可以：

   

   给角色分配权限

   

   移除角色权限

   

   查询角色权限

   

   4.3 产品(Client)管理模块

   

   Auth Center 需要支持多个业务系统接入。

   

   每个业务系统需要注册一个 OAuth Client。

   

   管理员可以通过界面完成以下操作：

   

   创建 Client

   

   创建时需要填写：

   

   Client名称

   

   Redirect URI

   

   Scope

   

   授权类型

   

   系统需要自动生成：

   

   client_id

   client_secret

   查询 Client

   

   管理员可以查看：

   

   Client列表

   

   Client详细信息

   

   修改 Client

   

   管理员可以修改：

   

   Redirect URI

   

   Scope

   

   Client名称

   

   删除 Client

   

   管理员可以删除一个 Client。

   

   删除后该 Client 将无法继续使用 OAuth 登录。

   

   4.4 OAuth2 授权模块

   

   Auth Center 需要实现 OAuth2 授权服务器能力。

   

   支持以下授权流程：

   

   Authorization Code Flow

   

   主要用于第三方系统登录。

   

   流程：

   

   用户访问业务系统

   ​      │

   点击 OAuth 登录

   ​      │

   跳转 Auth Center

   ​      │

   用户登录

   ​      │

   用户授权

   ​      │

   Auth Center 返回 authorization_code

   ​      │

   业务系统使用 code 获取 access_token

   Token管理

   

   系统需要支持：

   

   生成 Access Token

   

   刷新 Token

   

   Token 过期控制

   

   Token 校验

   

   用户信息接口

   

   Auth Center 需要提供接口用于返回 OAuth 用户信息。

   

   业务系统可以通过 Access Token 查询用户信息。

   

   返回信息包括：

   

   用户ID

   

   用户名

   

   邮箱

   

   用户状态

   

   五、Product System 功能需求

   

   Product System 是具体业务系统。

   

   需要实现用户体系和 OAuth 登录能力。

   

   5.1 用户管理模块

   

   Product System 需要维护自己的用户信息。

   

   功能包括：

   

   用户注册

   

   用户可以注册本系统账号。

   

   注册需要提供：

   

   用户名

   

   密码

   

   邮箱

   

   系统需要：

   

   校验用户名唯一

   

   创建用户

   

   用户登录

   

   用户可以使用账号密码登录。

   

   登录成功后系统需要生成访问令牌。

   

   用户信息查询

   

   用户可以查看自己的信息。

   

   用户信息修改

   

   用户可以修改：

   

   邮箱

   

   昵称

   

   密码

   

   5.2 权限管理模块

   

   Product System 也需要 RBAC 权限控制。

   

   功能包括：

   

   角色管理

   

   管理员可以：

   

   创建角色

   

   修改角色

   

   删除角色

   

   查询角色

   

   权限管理

   

   管理员可以：

   

   创建权限

   

   修改权限

   

   删除权限

   

   查询权限

   

   用户角色分配

   

   管理员可以给用户分配角色。

   

   5.3 OAuth 登录模块

   

   Product System 需要支持 OAuth 登录。

   

   流程如下：

   

   用户点击 OAuth 登录

   ​      │

   跳转 Auth Center

   ​      │

   用户登录

   ​      │

   Auth Center 返回 authorization_code

   ​      │

   Product System 使用 code 获取 access_token

   ​      │

   Product System 获取 OAuth 用户信息

   ​      │

   根据 OAuth 用户信息查找绑定关系

   ​      │

   登录成功

   5.4 OAuth 账号绑定模块

   

   Product System 需要支持 OAuth 账号绑定。

   

   OAuth绑定

   

   用户登录系统后可以绑定 OAuth 账号。

   

   绑定流程：

   

   用户点击绑定 OAuth

   ​      │

   跳转 Auth Center 授权

   ​      │

   Auth Center 返回用户ID

   ​      │

   Product System 创建绑定关系

   OAuth解绑

   

   用户可以解绑 OAuth 账号。

   

   解绑流程：

   

   用户进入账号安全页面

   ​      │

   点击解绑

   ​      │

   系统删除绑定关系

   绑定限制

   

   系统需要保证：

   

   一个 OAuth 账号只能绑定一个系统用户

   

   用户至少保留一种登录方式

   

   如果用户没有密码且解绑最后一个 OAuth，则不允许解绑。

   

   5.5 OAuth首次登录处理

   

   当 OAuth 用户第一次登录时：

   

   系统需要判断是否已有绑定。

   

   如果没有绑定，需要提供两种选择：

   

   创建新账号

   

   系统自动创建一个新用户，并绑定 OAuth。

   

   绑定已有账号

   

   用户输入已有账号密码。

   

   验证成功后绑定 OAuth。

   

   六、统一登录流程

   

   系统最终需要支持以下登录方式：

   

   本地账号密码登录

   

   流程：

   

   用户输入用户名密码

   ​      │

   系统验证

   ​      │

   生成访问令牌

   ​      │

   登录成功

   OAuth登录

   

   流程：

   

   用户点击 OAuth 登录

   ​      │

   跳转 Auth Center

   ​      │

   用户登录

   ​      │

   返回 authorization_code

   ​      │

   Product System 获取 access_token

   ​      │

   获取用户信息

   ​      │

   查找绑定关系

   ​      │

   生成访问令牌

   ​      │

   登录成功

   七、前端功能需求

   

   前端需要实现以下页面。

   

   Auth Center：

   

   登录页面

   

   注册页面

   

   用户管理页面

   

   角色管理页面

   

   权限管理页面

   

   Client管理页面

   

   Product System：

   

   登录页面

   

   注册页面

   

   用户中心页面

   

   OAuth绑定页面

   

   权限管理页面

   

   八、安全要求

   

   系统需要满足以下安全要求：

   

   所有接口使用 Token 认证

   

   Token 必须有过期时间

   

   OAuth授权必须校验 Client

   

   OAuth账号必须唯一绑定

   

   权限访问必须进行 RBAC 校验

   

   九、开发原则

   

   系统开发需要遵循：

   

   前后端完全分离

   

   所有接口使用 REST 风格

   

   使用统一返回格式

   

   所有接口必须进行权限控制

   

   所有敏感接口必须登录后访问

   