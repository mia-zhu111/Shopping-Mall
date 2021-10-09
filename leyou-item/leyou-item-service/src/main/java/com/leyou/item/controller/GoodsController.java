package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /*
        *根据条件分页查询Spu
     */
    //http://api.leyou.com/api/item/spu/page?key=&saleable=true&page=1&rows=5
    @GetMapping("spu/page")
    public ResponseEntity<PageResult<SpuBo>> querySpuByPage(
        @RequestParam(value="key", required = false) String key,
        @RequestParam(value="saleable", required = false) Boolean saleable,
        @RequestParam(value="page", defaultValue = "1") Integer page,
        @RequestParam(value="rows", defaultValue = "5") Integer rows
        ) {
        PageResult<SpuBo> pageResult = this.goodsService.querySpuByPage(key, saleable, page, rows);
        if(CollectionUtils.isEmpty(pageResult.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pageResult);
    }

    /*
        *新增商品
     */

    @PostMapping("goods")
    //json类型用@RequestBody注解接收
    public ResponseEntity<Void> saveGoods(@RequestBody SpuBo spuBo) {
        this.goodsService.saveGoods(spuBo);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /*
        *更新商品
        * http://api.leyou.com/api/item/goods
     */
    @PutMapping("goods")
    public ResponseEntity<Void> updataGoods(@RequestBody SpuBo spuBo) {
        this.goodsService.updataGoods(spuBo);
        //更新成功响应204
        return ResponseEntity.noContent().build();
    }

    /*
        * 根据spuId查询spuDetail
        * 商品编辑的回显
        *  oldGoods.spuDetail = await this.$http.loadData("/item/spu/detail/" + oldGoods.id); {}占位符
           oldGoods.skus = await this.$http.loadData("/item/sku/list?id=" + oldGoods.id);  ?id为普通参数
     */
    @GetMapping("spu/detail/{spuId}")
    public ResponseEntity<SpuDetail> querySpuDetailBySpuId(@PathVariable("spuId")Long spuId) {
        SpuDetail spuDetail = this.goodsService.querySpuDetailBySpuId(spuId);
        if(spuDetail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(spuDetail);
    }

    /*
        *根据spuId查询sku集合
     */
    @GetMapping("sku/list")
    public ResponseEntity<List<Sku>> querySkuBySpuId(@RequestParam("id")Long spuId) {
        List<Sku> skus = this.goodsService.querySkuBySpuId(spuId);
        if(CollectionUtils.isEmpty(skus)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(skus);
    }

    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    public ResponseEntity<Spu> querySpuById(@PathVariable("id") Long id){
        Spu spu = this.goodsService.querySpuById(id);
        if(spu == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(spu);
    }

    @GetMapping("sku/{skuId}")
    public  ResponseEntity<Sku> querySkuById(@PathVariable("skuId") Long skuId) {
        Sku sku = this.goodsService.querySkuById(skuId);
        if(sku == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(sku);

    }

}
