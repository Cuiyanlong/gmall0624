package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.SkuLsInfo;
import com.atguigu.gmall0624.bean.SkuLsParams;
import com.atguigu.gmall0624.bean.SkuLsResult;

public interface ListService {

    /**
     * 保存数据到es中
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 全文检索
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 商品的热度排名
     * @param skuId
     */
    void incrHotScore(String skuId);
}
