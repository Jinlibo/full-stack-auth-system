package com.product.app.dto;

import lombok.Data;

/**
 * 用户个人资料更新请求 DTO，包含可修改的邮箱、手机号、昵称和头像字段。
 * 所有字段均为可选，仅传入非 null 字段时才会更新对应列。
 */
@Data
public class UserUpdateRequest {

    /** 邮箱地址 */
    private String email;

    /** 手机号码 */
    private String phone;

    /** 用户昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;
}
