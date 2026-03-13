package com.auth.platform.controller;

import com.auth.platform.common.PageQuery;
import com.auth.platform.common.R;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.auth.platform.service.SysProductService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class SysProductController {

    private final SysProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:list')")
    public R<IPage<SysProduct>> pageProducts(PageQuery query) {
        return R.ok(productService.pageProducts(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:list')")
    public R<SysProduct> getProduct(@PathVariable Long id) {
        return R.ok(productService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public R<Void> createProduct(@Valid @RequestBody ProductRequest request) {
        productService.createProduct(request);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.ok();
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> toggleStatus(@PathVariable Long id) {
        productService.toggleStatus(id);
        return R.ok();
    }
}
