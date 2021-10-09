package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
//import org.elasticsearch.action.search.SearchRequest;
import com.leyou.search.pojo.SearchRequest;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    //获取分类名称，三级分类名称都需要，调用后台的微服务
    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;
    
    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        //首先注入CategoryClient  根据分类的id查询分类的名称，获取list集合
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        //首先注入BrandClient   根据品牌ID查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        //根据spuId查询所有的sku   遍历sku获取所有的价格放入新的数组中
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        //初始化一个价格集合  搜集所有spu的价格
        List<Long> prices = new ArrayList<>();
        //搜集sku的必要字段信息，因为不需要所有的sku信息。需要把sku对象转变为map
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());

            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            /*
            获取sku中的图片，数据库中的图片可能是多张，多张是以，进行分隔，所以也以逗号进行切割，
            返回图片数据，获取第一张图片
             */
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMapList.add(map);
        });

        //根据spu中cid3查询出所有的搜索规格参数
        List<SpecParam> params = this.specificationClient.queryParam(null, spu.getCid3(), null, true);

        //根据spuId查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        //将上面得到的spuDetail的json，进行反序列化
        Map<String,Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {});
        //把特殊的规格参数进行反序列化
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {});

        Map<String,Object> specs = new HashMap<>();
        params.forEach(param -> {
            //判断规格参数的类型是否是通用的规格参数
            if(param.getGeneric()) {
                //如果是通用类型的参数，从genericSpecMap获取规格参数
                String value = genericSpecMap.get(param.getId().toString()).toString();
                //数字应当是区间，判断是否是数值类型，如果是数值应该返回一个区间
                if(param.getNumeric()) {
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            }else {
                //如果是特殊的规格参数，从specialSpecMap获取值
                List<Object> value = specialSpecMap.get(param.getId().toString());
                specs.put(param.getName(), value);
            }
        });

        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());

        //拼接all字段  spu拼接上分类的名称和品牌的名称
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        //获取spu下的所有sku价格，应该先获取所有的sku
        goods.setPrice(prices);
        /*
            * 获取spu下的所有sku，并转化成json字符串
            * 需要sku中的 id 图片 价格 标题    使用List<Map<id/title/price/image, Object>>
         */
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        //获取所有查询的规格参数{name, value}
        goods.setSpecs(specs);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }


    public SearchResult<Goods> search(SearchRequest request) {
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(request.getKey())) {
            return null;
        }

        //自定义查询构建器
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 1、对key进行全文检索查询
        //QueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        BoolQueryBuilder basicQuery = buildBoolQueryBuilder(request);
        queryBuilder.withQuery(basicQuery);

        // 2、添加结果集过滤  通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(
                new String[]{"id","skus","subTitle"}, null));

        // 3、分页
        // 准备分页参数 分页页码从0开始 所以页码减1
        int page = request.getPage();
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page - 1, size));

        //添加分类和品牌的聚合
        String categoryAggName = "categories";
        String brandAggName = "brands";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4、查询，获取结果
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        //获取聚合结果集并解析，需要List<Map<String, Object>>
        List<Map<String, Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));
        //在用户选择好规格参数 确定分类之后再做聚合  只有一个分类的时候才做规格参数聚合
        List<Map<String, Object>> specs = null;
        if( ! CollectionUtils.isEmpty(categories) && categories.size() == 1) {
            //对规格参数进行聚合 分类id
            specs = getParamAggResult((Long)categories.get(0).get("id"), basicQuery);
        }

        // 封装结果并返回
        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(), categories, brands, specs);
    }

    //构建布尔查询
    private BoolQueryBuilder buildBoolQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //给布尔查询添加基本的查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));
        //添加过滤条件
        //获取用户选择的过滤信息
        Map<String, Object> filter = request.getFilter();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            if(StringUtils.equals("品牌", key)) {
                key = "brandId";
            }else if(StringUtils.equals("分类", key)) {
                key = "cid3";
            }else {
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery("key", entry.getValue()));
        }
        return boolQueryBuilder;
    }

    //根据查询条件聚合规格参数
    private List<Map<String, Object>> getParamAggResult(Long cid, QueryBuilder basicQuery) {
        //自定义查询对象构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加基本查询条件
        queryBuilder.withQuery(basicQuery);

        //查询要聚合的规格参数
        List<SpecParam> params = this.specificationClient.queryParam(null, cid, null, true);

        //添加规格参数的聚合
        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("speces." + param.getName() + ".keyword"));
        });

        //添加结果集过滤  不需要普通结果集 不包含任何字段
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

        //执行聚合查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        //解析聚合结果集,map key-聚合名称(规格参数名) value-聚合对象
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();

        //处理结果集 变成 List<Map<String, Object>>，每一个聚合处理成map
        List<Map<String, Object>> specs = new ArrayList<>();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            //{k, options是一个集合}
            Map<String, Object> map = new HashMap<>();
            map.put("k", entry.getKey());
            //初始化一个options集合，收集桶中的key
            List<String> options = new ArrayList<>();
            //获取聚合
            StringTerms terms = (StringTerms)entry.getValue();
            //获取桶集合
            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    /*
        解析品牌的聚合结果集 拿到里面的桶进行解析 词条聚合所以是LongTerms  StringTerms
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms)aggregation;

        List<Brand> brands = new ArrayList<>();

        //获取聚合中的桶  遍历桶 获取key组成数组 得到brand集合
        terms.getBuckets().forEach(bucket -> {
            Brand brand = this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
            brands.add(brand);
        });
        //根据品牌Id查询品牌  遍历bids
        return brands;
    }

    /*
        解析分类的分页结果集
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms)aggregation;

        /*
            获取桶的集合 转化成List<Map<String, Object>>
            通过stream表达是获取桶的集合 遍历 把桶的元素转化为Map<String, Object>
         */
        return terms.getBuckets().stream().map(bucket -> {
            //初始化一个map
            Map<String, Object> map = new HashMap<>();
            //获取桶中分类的id(Key)
            Long id = bucket.getKeyAsNumber().longValue(); //获取分类Id
            //根据分类Id查询分类名称
            List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(id));
            map.put("id", id);
            map.put("name", names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    public void save(Long id) throws IOException {
        Spu spu = this.goodsClient.querySpuById(id);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
        /*
            1 保存goods 没有goods就调用buildGoods方法创建
            2 buildGoods没有spu 就调用goodsClient方法通过id得到spu
         */
    }

    public void delete(Long id) {
        this.goodsRepository.deleteById(id);
    }
}
