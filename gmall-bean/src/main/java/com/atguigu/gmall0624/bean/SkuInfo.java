package com.atguigu.gmall0624.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuInfo implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    String id;

    @Column
    String spuId;

    @Column
    BigDecimal price;

    @Column
    String skuName;

    @Column
    BigDecimal weight;

    @Column
    String skuDesc;

    @Column
    String catalog3Id;

    @Column
    String skuDefaultImg;

    // 封装图片
    @Transient
    List<SkuImage> skuImageList;

    // 封装平台属性
    @Transient
    List<SkuAttrValue> skuAttrValueList;
    // 封装销售属性
    @Transient
    List<SkuSaleAttrValue> skuSaleAttrValueList;

}
