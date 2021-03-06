package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GoodsApi {
    /*
        * 根据spuId查询spuDetail
        * 商品编辑的回显
        *  oldGoods.spuDetail = await this.$http.loadData("/item/spu/detail/" + oldGoods.id); {}占位符
           oldGoods.skus = await this.$http.loadData("/item/sku/list?id=" + oldGoods.id);  ?id为普通参数
     */
    @GetMapping("spu/detail/{spuId}")
    public SpuDetail querySpuDetailBySpuId(@PathVariable("spuId")Long spuId);

    /*
     *根据条件分页查询Spu
     */
    //http://api.leyou.com/api/item/spu/page?key=&saleable=true&page=1&rows=5
    @GetMapping("spu/page")
    public PageResult<SpuBo> querySpuByPage(
            @RequestParam(value="key", required = false) String key,
            @RequestParam(value="saleable", required = false) Boolean saleable,
            @RequestParam(value="page", defaultValue = "1") Integer page,
            @RequestParam(value="rows", defaultValue = "5") Integer rows
    );

    /*
     *根据spuId查询sku集合
     */
    @GetMapping("sku/list")
    public List<Sku> querySkuBySpuId(@RequestParam("id")Long spuId);

    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    public Spu querySpuById(@PathVariable("id") Long id);

    @GetMapping("sku/{skuId}")
    public Sku querySkuById(@PathVariable("skuId") Long skuId);

}
