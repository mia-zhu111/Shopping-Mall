package com.leyou.search.client;

import com.leyou.item.api.GoodsApi;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("item-service")    //指定为item-service微服务
//public interface GoodsClient {
//    /*
//        * 下面直接调用item-service下面的controller方法
//        * 调用哪个方法就跟哪个方法一样
//     */
//    /*
//        * 根据spuId查询spuDetail
//        * 商品编辑的回显
//        *  oldGoods.spuDetail = await this.$http.loadData("/item/spu/detail/" + oldGoods.id); {}占位符
//           oldGoods.skus = await this.$http.loadData("/item/sku/list?id=" + oldGoods.id);  ?id为普通参数
//     */
//    @GetMapping("spu/detail/{spuId}")
//    public SpuDetail querySpuDetailBySpuId(@PathVariable("spuId")Long spuId);
//}
public interface GoodsClient extends GoodsApi {

}
