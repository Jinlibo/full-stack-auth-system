package com.product.app.common;

import lombok.Data;

/**
 * 通用分页查询参数封装类，包含页码、每页条数及关键词搜索字段。
 */
@Data
public class PageQuery {

    /** 当前页码，默认第 1 页 */
    private Integer pageNum = 1;

    /** 每页显示条数，默认 10 条 */
    private Integer pageSize = 10;

    /** 模糊搜索关键词 */
    private String keyword;
}
