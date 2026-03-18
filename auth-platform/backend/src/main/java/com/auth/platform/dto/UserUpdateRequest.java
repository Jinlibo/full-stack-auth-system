package com.auth.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息更新请求数据传输对象
 * <p>封装管理员更新用户信息时提交的数据，所有字段均为可选。
 * 服务层采用局部更新策略：仅更新请求中不为 null 的字段，
 * 角色列表（roleIds）不为 null 时执行全量替换。</p>
 */
@Data
public class UserUpdateRequest {
    /** 用户邮箱，为 null 时不更新 */
    private String email;
    /** 用户手机号，为 null 时不更新 */
    private String phone;
    /** 用户昵称，为 null 时不更新 */
    private String nickname;
    /** 用户头像地址，为 null 时不更新 */
    private String avatar;
    /** 账号状态：1=启用，0=禁用，为 null 时不更新 */
    private Integer status;
    /** 角色 ID 列表，不为 null 时将全量替换用户的角色关联（空数组表示清空所有角色） */
    private List<Long> roleIds;
}
