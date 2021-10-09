package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecificationService {

    @Autowired(required = false)
    private SpecGroupMapper groupMapper;
    @Autowired(required = false)
    private SpecParamMapper paramMapper;

    /*
        *根据分类id查询参数组
        * 首先确认使用什么通用mapper方法
     */
    public List<SpecGroup> querygroupByCid(Long cid) {
        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        return this.groupMapper.select(record);
    }

    /*
     *根据分类id查询参数组
     * 首先确认使用什么通用mapper方法
     */
    public List<SpecParam> queryParam(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam record = new SpecParam();
        record.setGroupId(gid);
        record.setCid(cid);
        record.setGeneric(generic);
        record.setSearching(searching);
        return this.paramMapper.select(record);
    }

    public List<SpecGroup> queryGroupsWithParam(Long cid) {
        //首先查询组，再查询组下面的规格参数
        List<SpecGroup> groups = this.querygroupByCid(cid);
        groups.forEach(group -> {
            List<SpecParam> params = this.queryParam(group.getId(), null, null, null);
            group.setParams(params);
        });
        return groups;
    }
}
