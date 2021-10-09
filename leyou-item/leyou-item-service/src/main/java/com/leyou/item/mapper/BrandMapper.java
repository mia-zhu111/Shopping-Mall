package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand> {

    /*
    sql语句中有增删改查字段，在项目有他们的注解，将sql语句写在注解中，玩配置就是玩注解
    新增商品分类和品牌中间表数据
     * @param cid 商品分类id
     * @param bid 品牌id
     * @return
     */
    @Insert("INSERT INTO tb_category_brand (category_id, brand_id) values (#{cid},#{bid})")
    void insertCategoryAndBrand(@Param("cid")Long cid, @Param("bid")Long id);

    //使用sql语句进行表关联查询
    @Select("SELECT b.* from tb_brand b INNER JOIN tb_category_brand cb on b.id=cb.brand_id where cb.category_id=#{cid}")
    List<Brand> selectBrandsByCid(Long cid);
}
