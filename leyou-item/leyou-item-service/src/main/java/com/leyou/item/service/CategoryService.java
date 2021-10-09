package com.leyou.item.service;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    /*
    在方法中加注释：
    根据父节点查询子节点
     */

    @Autowired(required = false)
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoriesByPid(Long pid) {

        Category record = new Category();
        record.setParentId(pid);
        //有条件查询，并返回集合，查询一级目录
        return this.categoryMapper.select(record);
    }

    public List<String> queryNamesByIds(List<Long> ids) {
        List<Category> list = this.categoryMapper.selectByIdList(ids);
        List<String> names = new ArrayList<>();
        for (Category category : list) {
            names.add(category.getName());
        }
        return names;
        // return list.stream().map(category -> category.getName()).collect(Collectors.toList());
    }
}
