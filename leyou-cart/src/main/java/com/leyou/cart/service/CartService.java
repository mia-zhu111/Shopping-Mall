package com.leyou.cart.service;

import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    static final String KEY_PREFIX = "user:cart:";

    static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public void addCart(Cart cart) {
        // 获取登录用户
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        // Redis的key
        String key = KEY_PREFIX + userInfo.getId();
        // 获取hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 查询是否存在
        Long skuId = cart.getSkuId();
        Integer num = cart.getNum();
        Boolean boo = hashOps.hasKey(skuId.toString());

        if (boo) {
            // 存在，获取购物车数据
            //cart类型的json
            String json = hashOps.get(skuId.toString()).toString();
            cart = JsonUtils.parse(json, Cart.class);
            // 修改购物车数量
            cart.setNum(cart.getNum() + num);
            hashOps.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));
        } else {
            // 不存在，新增购物车数据
            cart.setUserId(userInfo.getId());
            // 其它商品信息，需要查询商品服务  查询sku需要调用item-service，在item-service中封装一个querySkuByid
            Sku sku = this.goodsClient.querySkuById(skuId);
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
        }
        // 将购物车数据写入redis
        //hashOps.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //先判断用户是否有购物车记录
        if(this.redisTemplate.hasKey(KEY_PREFIX + userInfo.getId())) {
            return null;
        }

        //获取用户的购物车记录
        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX + userInfo.getId());
        //获取购物车map中所有的Cart值集合
        List<Object> cartsJson = hashOperations.values();

        //如果购物车集合为空，返回null
        if(CollectionUtils.isEmpty(cartsJson)) {
            return null;
        }
        //把List<Object>集合转化为List<Cart>集合
        return cartsJson.stream().map(cartJson -> JsonUtils.parse(cartsJson.toString(), Cart.class)).collect(Collectors.toList());
    }

    public void updateCarts(Cart cart) {
        // 获取登陆信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        if(this.redisTemplate.hasKey(KEY_PREFIX + userInfo.getId())) {
            return ;
        }
        String key = KEY_PREFIX + userInfo.getId();
        // 获取hash操作对象
        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(key);
        // 获取购物车信息
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
        // 更新数量
        cart1.setNum(cart.getNum());
        // 写入购物车
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart1));
    }

    public void deleteCart(String skuId) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if(this.redisTemplate.hasKey(KEY_PREFIX + userInfo.getId())) {
            return ;
        }

        String key = KEY_PREFIX + userInfo.getId();

        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(key);
        hashOperations.delete(skuId);

    }
}
