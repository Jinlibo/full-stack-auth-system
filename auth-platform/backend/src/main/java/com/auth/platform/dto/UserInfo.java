package com.auth.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息数据传输对象
 * <p>用于向前端返回用户的基本资料、账号状态以及关联的角色和权限列表。
 * 该对象不包含密码等敏感字段，可安全地序列化后通过接口返回。</p>
 */
@Data
public class UserInfo {
    /** 用户主键 ID */
    private Long id;
    /** 用户名（登录名） */
    private String username;
    /** 用户邮箱 */
    private String email;
    /** 用户手机号 */
    private String phone;
    /** 用户昵称（显示名称） */
    private String nickname;
    /** 用户头像地址 */
    private String avatar;
    /** 账号状态：1=正常，0=禁用 */
    private Integer status;
    /** 用户拥有的角色标识列表，如 ["SUPER_ADMIN", "USER"] */
    private List<String> roles;
    /** 用户拥有的权限标识列表，如 ["system:user:add", "product:list"] */
    private List<String> permissions;
}
