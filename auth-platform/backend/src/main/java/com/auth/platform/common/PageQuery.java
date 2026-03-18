package com.auth.platform.common;

import lombok.Data;

/**
 * 分页查询通用参数封装类
 *
 * <p>用于接收前端传入的分页和关键字搜索参数，各 Controller 的列表查询接口
 * 统一使用此类作为查询参数，避免在每个接口重复定义分页字段。
 *
 * <p>使用方式示例：
 * <pre>GET /api/users?pageNum=2&pageSize=20&keyword=admin</pre>
 *
 * @author auth-platform
 */
@Data
public class PageQuery {

    /** 当前页码，默认第 1 页，从 1 开始计数 */
    private Integer pageNum = 1;

    /** 每页显示条数，默认 10 条 */
    private Integer pageSize = 10;

    /** 关键字搜索词，为空时查询全部；不为空时按各业务接口定义的字段模糊匹配 */
    private String keyword;
}
