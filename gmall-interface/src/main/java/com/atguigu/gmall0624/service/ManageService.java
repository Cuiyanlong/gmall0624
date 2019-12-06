package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.*;

import java.util.List;

public interface ManageService {

    //查询所有的一级分类
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类Id，查询二级分类数据
     * @param catalog1Id
     * @return
             */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类Id，查询三级分类数据
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);


    List<BaseAttrInfo> getAttrInfoList(String catalog3Id);

    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    BaseAttrInfo getAttrInfo(String attrId);

}
