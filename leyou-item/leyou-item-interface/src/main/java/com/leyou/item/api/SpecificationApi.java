package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("spec")  //定义全局路径
public interface SpecificationApi {


    /*
        * 根据条件查询规格参数
     */
    // 请求路径 http://api.leyou.com/api/item/spec/params?gid=1
    @GetMapping("params")
    public List<SpecParam> queryParam(
            @RequestParam(value = "gid", required = false)Long gid,
            @RequestParam(value = "cid", required = false)Long cid,
            @RequestParam(value = "generic", required = false)Boolean generic,
            @RequestParam(value = "searching", required = false)Boolean searching
    );

    @GetMapping("group/param/{cid}")
    public List<SpecGroup> queryGoupsWithParam(@PathVariable("cid")Long cid);
}
