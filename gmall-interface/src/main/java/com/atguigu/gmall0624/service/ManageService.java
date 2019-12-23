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


    List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo);

    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    BaseAttrInfo getAttrInfo(String attrId);

    /**
     *
     * @param catalog3Id
     * @return
     */
    List<SpuInfo> getSpuList(String catalog3Id);

    /**
     *根据Spuinfo属性查询spuinfo
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuList(SpuInfo spuInfo);

    /**
     * 查询所有销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spuInfo数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据属性查找图片集合
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 根据三级分类id查询平台信息
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(String catalog3Id);

    /**
     * 根据spuID查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     *大保存
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据skuId查询skuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 通过spuId，skuid查询销售属性集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 通过spuid查询sku与销售属性中间表集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 通过平台属性Id集合 查询平台属性集合
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) ;
}
