-- =============================================
-- Auth Platform 数据库初始化脚本
-- =============================================
CREATE DATABASE IF NOT EXISTS auth_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE auth_platform;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(256) NOT NULL COMMENT '密码(BCrypt)',
    email      VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    nickname   VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    avatar     VARCHAR(512)          DEFAULT NULL COMMENT '头像URL',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删 1-已删',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE = InnoDB COMMENT ='系统用户表';

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name  VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_key   VARCHAR(64) NOT NULL UNIQUE COMMENT '角色标识',
    sort_order INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    remark     VARCHAR(256)         DEFAULT NULL COMMENT '备注',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT ='角色表';

-- 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(64)  NOT NULL COMMENT '权限名称',
    permission_key  VARCHAR(128) NOT NULL UNIQUE COMMENT '权限标识',
    parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID',
    type            TINYINT      NOT NULL DEFAULT 1 COMMENT '类型: 1-菜单 2-按钮 3-API',
    path            VARCHAR(256)          DEFAULT NULL COMMENT '路由路径',
    icon            VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT ='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE = InnoDB COMMENT ='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_permission_id (permission_id)
) ENGINE = InnoDB COMMENT ='角色权限关联表';

-- 产品表
DROP TABLE IF EXISTS sys_product;
CREATE TABLE sys_product
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name   VARCHAR(128) NOT NULL COMMENT '产品名称',
    product_key    VARCHAR(64)  NOT NULL UNIQUE COMMENT '产品标识(即OAuth2 client_id)',
    product_secret VARCHAR(256) NOT NULL COMMENT '产品密钥(即OAuth2 client_secret)',
    description    VARCHAR(512)          DEFAULT NULL COMMENT '产品描述',
    homepage_url   VARCHAR(512)          DEFAULT NULL COMMENT '产品首页',
    redirect_uris  TEXT                  DEFAULT NULL COMMENT '回调URI(JSON数组)',
    logo_url       VARCHAR(512)          DEFAULT NULL COMMENT 'Logo',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_key (product_key)
) ENGINE = InnoDB COMMENT ='产品表(同时作为OAuth2客户端)';

-- Spring Authorization Server 所需表
-- oauth2_registered_client
DROP TABLE IF EXISTS oauth2_registered_client;
CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)                            NOT NULL PRIMARY KEY,
    client_id                     VARCHAR(100)                            NOT NULL,
    client_id_issued_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(200)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(200)                            NOT NULL,
    client_authentication_methods VARCHAR(1000)                           NOT NULL,
    authorization_grant_types     VARCHAR(1000)                           NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000)                           NOT NULL,
    client_settings               VARCHAR(2000)                           NOT NULL,
    token_settings                VARCHAR(2000)                           NOT NULL,
    UNIQUE KEY uk_client_id (client_id)
) ENGINE = InnoDB COMMENT ='OAuth2注册客户端';

-- oauth2_authorization
DROP TABLE IF EXISTS oauth2_authorization;
CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100) NOT NULL PRIMARY KEY,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    TEXT          DEFAULT NULL,
    state                         VARCHAR(500)  DEFAULT NULL,
    authorization_code_value      TEXT          DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMP     DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP     DEFAULT NULL,
    authorization_code_metadata   TEXT          DEFAULT NULL,
    access_token_value            TEXT          DEFAULT NULL,
    access_token_issued_at        TIMESTAMP     DEFAULT NULL,
    access_token_expires_at       TIMESTAMP     DEFAULT NULL,
    access_token_metadata         TEXT          DEFAULT NULL,
    access_token_type             VARCHAR(100)  DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value           TEXT          DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMP     DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMP     DEFAULT NULL,
    oidc_id_token_metadata        TEXT          DEFAULT NULL,
    oidc_id_token_claims          TEXT          DEFAULT NULL,
    refresh_token_value           TEXT          DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMP     DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMP     DEFAULT NULL,
    refresh_token_metadata        TEXT          DEFAULT NULL,
    user_code_value               TEXT          DEFAULT NULL,
    user_code_issued_at           TIMESTAMP     DEFAULT NULL,
    user_code_expires_at          TIMESTAMP     DEFAULT NULL,
    user_code_metadata            TEXT          DEFAULT NULL,
    device_code_value             TEXT          DEFAULT NULL,
    device_code_issued_at         TIMESTAMP     DEFAULT NULL,
    device_code_expires_at        TIMESTAMP     DEFAULT NULL,
    device_code_metadata          TEXT          DEFAULT NULL
) ENGINE = InnoDB COMMENT ='OAuth2授权信息';

-- oauth2_authorization_consent
DROP TABLE IF EXISTS oauth2_authorization_consent;
CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE = InnoDB COMMENT ='OAuth2授权同意';

-- =============================================
-- 初始化数据
-- =============================================

-- 默认管理员 (密码: admin123)
--
-- 重要：password 字段存储的是占位符而非真实 BCrypt 哈希。
-- Spring Boot 启动时，DataInitializer 会检测到此占位符无法通过 BCrypt 验证，
-- 并自动将其替换为 passwordEncoder.encode("admin123") 的正确哈希值。
-- 这样设计的好处：无论 SQL 被重复执行多少次，只需重启应用即可恢复正确密码，
-- 无需在 SQL 中硬编码一个可能因环境差异而失效的 BCrypt 哈希。
INSERT INTO sys_user (username, password, email, nickname, status)
VALUES ('admin', 'PLACEHOLDER_FIXED_BY_DATAINITIALIZER', 'admin@example.com', '超级管理员', 1);

-- 默认角色
INSERT INTO sys_role (role_name, role_key, sort_order, remark)
VALUES ('超级管理员', 'SUPER_ADMIN', 1, '拥有所有权限'),
       ('产品管理员', 'PRODUCT_ADMIN', 2, '产品管理权限'),
       ('普通用户', 'USER', 3, '基础权限');

-- 默认权限
INSERT INTO sys_permission (permission_name, permission_key, parent_id, type, path, sort_order)
VALUES ('系统管理', 'system', 0, 1, '/system', 1),
       ('用户管理', 'system:user', 1, 1, '/system/user', 1),
       ('用户查询', 'system:user:query', 2, 3, NULL, 1),
       ('用户新增', 'system:user:add', 2, 2, NULL, 2),
       ('用户修改', 'system:user:edit', 2, 2, NULL, 3),
       ('用户删除', 'system:user:delete', 2, 2, NULL, 4),
       ('角色管理', 'system:role', 1, 1, '/system/role', 2),
       ('角色查询', 'system:role:query', 7, 3, NULL, 1),
       ('角色新增', 'system:role:add', 7, 2, NULL, 2),
       ('角色修改', 'system:role:edit', 7, 2, NULL, 3),
       ('角色删除', 'system:role:delete', 7, 2, NULL, 4),
       ('权限管理', 'system:permission', 1, 1, '/system/permission', 3),
       ('产品管理', 'product', 0, 1, '/product', 2),
       ('产品列表', 'product:list', 13, 1, '/product/list', 1),
       ('产品新增', 'product:add', 13, 2, NULL, 2),
       ('产品修改', 'product:edit', 13, 2, NULL, 3),
       ('产品删除', 'product:delete', 13, 2, NULL, 4);

-- 管理员拥有所有角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission;

-- 产品管理员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id
FROM sys_permission
WHERE permission_key LIKE 'product%';

-- ============================================================
-- 初始化示例产品（即 product-app 的 OAuth2 客户端）
--
-- 重要：sys_product.product_secret 必须存储【明文密钥】，
-- 供管理页面展示给开发者复制到 product-app/application.yml。
-- BCrypt 加密发生在 oauth2_registered_client 表（见下方）。
--
-- 可配置流程：
--   1. 在认证平台"产品管理"页面创建/查看产品
--      → 页面会显示明文的 Client ID（product_key）和 Client Secret（product_secret）
--   2. 将这两个值填写到 product-app 的 application.yml：
--        oauth2.client.client-id: <product_key>
--        oauth2.client.client-secret: <product_secret>
--   3. 重启 product-app，OAuth2 授权登录即可正常使用
-- ============================================================
INSERT INTO sys_product (product_name, product_key, product_secret, description, redirect_uris, status)
VALUES ('示例产品应用', 'product-app', 'admin123',
        '第二个项目-产品应用（默认密钥: admin123，可在产品管理页面查看或重置）',
        '["http://localhost:5174/oauth/callback"]', 1);

-- 同步注册到OAuth2客户端表
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES ('1', 'product-app', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm1z98LYEqnZPmCu9P1W', '示例产品应用',
        'client_secret_basic,client_secret_post', 'authorization_code,refresh_token',
        'http://localhost:5174/oauth/callback', 'openid,profile,email',
        '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
        '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",86400.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}');
