package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysProductService extends IService<SysProduct> {
    IPage<SysProduct> pageProducts(PageQuery query);

    void createProduct(ProductRequest request);

    void updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    void toggleStatus(Long id);
}
