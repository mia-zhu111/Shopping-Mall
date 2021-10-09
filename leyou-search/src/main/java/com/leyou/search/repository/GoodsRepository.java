package com.leyou.search.repository;

import com.leyou.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

//通过接口做增删改查数据
public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
}
