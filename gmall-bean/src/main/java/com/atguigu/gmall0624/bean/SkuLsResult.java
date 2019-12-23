package com.atguigu.gmall0624.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {
    //页面显示商品集合
    List<SkuLsInfo> skuLsInfoList;
    //查询出来的总条数
    long total;
    //总页数
    long totalPages;
    //属性值id的集合
    List<String> attrValueIdList;
}
