package com.atguigu.gmall0624.manage.mapper;

import com.atguigu.gmall0624.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    //根据三级分类id查询数据
    List<BaseAttrInfo> selectBaseAttrInfoListByCatalog3Id(String catalog3Id);
    //通过平台属性Id集合 查询数据
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}
