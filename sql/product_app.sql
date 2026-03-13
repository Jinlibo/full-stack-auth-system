-- =============================================
-- Product App 数据库初始化脚本
-- =============================================
CREATE DATABASE IF NOT EXISTS product_app DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE product_app;

-- 用户表
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(256)         DEFAULT NULL COMMENT '密码(BCrypt), OAuth用户可为空',
    email      VARCHAR(128)         DEFAULT NULL COMMENT '邮箱',
    phone      VARCHAR(20)          DEFAULT NULL COMMENT '手机号',
    nickname   VARCHAR(64)          DEFAULT NULL COMMENT '昵称',
    avatar     VARCHAR(512)         DEFAULT NULL COMMENT '头像URL',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE = InnoDB COMMENT ='应用用户表';

-- OAuth绑定表 (多个第三方账号映射到同一用户)
DROP TABLE IF EXISTS app_user_oauth;
CREATE TABLE app_user_oauth
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '关联的本地用户ID',
    oauth_provider  VARCHAR(64)  NOT NULL COMMENT 'OAuth提供者标识(如auth-platform)',
    oauth_uid       VARCHAR(256) NOT NULL COMMENT 'OAuth用户唯一ID',
    oauth_username  VARCHAR(128)          DEFAULT NULL COMMENT 'OAuth用户名',
    oauth_avatar    VARCHAR(512)          DEFAULT NULL COMMENT 'OAuth头像',
    access_token    VARCHAR(1024)         DEFAULT NULL COMMENT 'Access Token',
    refresh_token   VARCHAR(1024)         DEFAULT NULL COMMENT 'Refresh Token',
    token_expire_at DATETIME              DEFAULT NULL COMMENT 'Token过期时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_uid (oauth_provider, oauth_uid),
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB COMMENT ='OAuth第三方绑定表';

-- 角色表
DROP TABLE IF EXISTS app_role;
CREATE TABLE app_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name  VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_key   VARCHAR(64) NOT NULL UNIQUE COMMENT '角色标识',
    sort_order INT         NOT NULL DEFAULT 0,
    status     TINYINT     NOT NULL DEFAULT 1,
    remark     VARCHAR(256)         DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT ='角色表';

-- 权限表
DROP TABLE IF EXISTS app_permission;
CREATE TABLE app_permission
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(64)  NOT NULL,
    permission_key  VARCHAR(128) NOT NULL UNIQUE,
    parent_id       BIGINT       NOT NULL DEFAULT 0,
    type            TINYINT      NOT NULL DEFAULT 1 COMMENT '1-菜单 2-按钮 3-API',
    path            VARCHAR(256)          DEFAULT NULL,
    icon            VARCHAR(64)           DEFAULT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT ='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS app_user_role;
CREATE TABLE app_user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB COMMENT ='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS app_role_permission;
CREATE TABLE app_role_permission
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE = InnoDB COMMENT ='角色权限关联表';

-- =============================================
-- 初始化数据
-- =============================================
INSERT INTO app_user (username, password, email, nickname, status)
VALUES
-- 密码: admin123 (BCrypt, cost=10)
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@product.com', '管理员', 1);

INSERT INTO app_role (role_name, role_key, sort_order, remark)
VALUES ('管理员', 'ADMIN', 1, '应用管理员'),
       ('普通用户', 'USER', 2, '普通用户');

INSERT INTO app_permission (permission_name, permission_key, parent_id, type, path, sort_order)
VALUES ('仪表盘', 'dashboard', 0, 1, '/dashboard', 1),
       ('用户管理', 'user', 0, 1, '/user', 2),
       ('用户列表', 'user:list', 2, 1, '/user/list', 1),
       ('用户编辑', 'user:edit', 2, 2, NULL, 2),
       ('角色管理', 'role', 0, 1, '/role', 3),
       ('个人中心', 'profile', 0, 1, '/profile', 4);

INSERT INTO app_user_role (user_id, role_id)
VALUES (1, 1);

INSERT INTO app_role_permission (role_id, permission_id)
SELECT 1, id
FROM app_permission;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT 2, id
FROM app_permission
WHERE permission_key IN ('dashboard', 'profile');
