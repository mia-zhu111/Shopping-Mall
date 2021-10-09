package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("brand")
public interface BrandApi {

    @GetMapping("{id}") //占位符参数
    public Brand queryBrandById(@PathVariable("id")Long id);

}
