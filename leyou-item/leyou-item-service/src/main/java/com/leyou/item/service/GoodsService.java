package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired(required = false)
    private SpuMapper spuMapper;

    @Autowired(required = false)
    private BrandMapper brandMapper;

    @Autowired(required = false)
    private CategoryService categoryService;

    @Autowired(required = false)
    private SpuDetailMapper spuDetailMapper;

    @Autowired(required = false)
    private SkuMapper skuMapper;

    @Autowired(required = false)
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private GoodsService goodsService;

    public PageResult<SpuBo> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        //添加查询条件
        if (StringUtils.isNotBlank(key)) {
            //模糊查询，key为空就不需要查询
            criteria.andLike("title", "%" + key + "%");
        }

        //是否上架/下架 添加上架/下架的过滤条件
        if (saleable != null) {
            //对应数据库中只有0 和 1，求交集
            criteria.andEqualTo("saleable", saleable);
        }

        //添加分页
        PageHelper.startPage(page, rows);

        //执行查询，获取spu集合
        List<Spu> spus = this.spuMapper.selectByExample(example);
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        //spu集合转化为spuBo集合
        List<SpuBo> spuBos = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu, spuBo);

            //查询品牌名称
            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());
            //查询分类名称
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "-"));
            return spuBo;

        }).collect(Collectors.toList());

        //返回pageresult<SpuBo>
        return new PageResult<>(pageInfo.getTotal(), spuBos);
    }

    /*
        *新增商品
     */
    @Transactional
    public void saveGoods(SpuBo spuBo) {
        //先新增spu，使用spu的mapper
        //对于表单中没有的参数需要手动设置默认值
        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        this.spuMapper.insertSelective(spuBo);

        //再新增spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        this.spuDetailMapper.insertSelective(spuDetail);

        //新增sku, 传入的参数为sku集合， 遍历skus
        //sku和stock是同一张表，所以没新增一个sku，都要更新stock
        // 新增stock
        saveSkuAndStock(spuBo);

        //发送消息
        sendMsg("insert", spuBo.getId());


    }

    private void sendMsg(String type, Long id) {
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }


    //提取出方法进行调用
    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku -> {
            sku.setId(null);
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);
            //在sku中传入了stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.insertSelective(stock);
        });
    }

    /*
         * 根据spuId查询spuDetail
         * 商品编辑的回显
         * oldGoods.spuDetail = await this.$http.loadData("/item/spu/detail/" + oldGoods.id);
           oldGoods.skus = await this.$http.loadData("/item/sku/list?id=" + oldGoods.id);
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        //在spuDetail中主键就是spuId，所以选择mapper中的主键方法
        return this.spuDetailMapper.selectByPrimaryKey(spuId);
    }

    /*
        *根据spuId查询skus集合
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku record = new Sku();
        record.setSpuId(spuId);
        List<Sku> skus = this.skuMapper.select(record);
        skus.forEach(sku -> {
            //在stock里面skuId就是主键
            Stock stock = this.stockMapper.selectByPrimaryKey(sku.getId());
            sku.setStock(stock.getStock());
        });
        return skus;
    }

    /*
     *更新商品
     * http://api.leyou.com/api/item/goods
     * 分析需要更新哪些表 spu spuDetail sku
     */
    @Transactional
    public void updataGoods(SpuBo spuBo) {
        //操作sku相关。
        //根据spuId查询要删除的sku
        Sku record = new Sku();
        record.setSpuId(spuBo.getId());
        List<Sku> skus = this.skuMapper.select(record);
        skus.forEach(sku -> {
            //删除stock
            this.skuMapper.deleteByPrimaryKey(sku.getId());
        });

        //删除sku  根据spuId进行删除
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        this.skuMapper.delete(sku);

        //都需要根据skus遍历进行更新
        //新增sku
        //新增stock
        this.saveSkuAndStock(spuBo);

        //更新spu和spuDetail
        //事件不能随意更新
        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        spuBo.setValid(null);
        spuBo.setSaleable(null);
        this.spuMapper.updateByPrimaryKeySelective(spuBo);


        this.spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
        sendMsg("update", spuBo.getId());

    }

    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    public Sku querySkuById(Long skuId) {
        return this.skuMapper.selectByPrimaryKey(skuId);
    }
}


