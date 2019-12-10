package com.atguigu.gmall0624.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class SkuAttrValue implements Serializable{

    @Id
    @Column
    String id;

    // 平台属性Id baseAttrInfo.id
    @Column
    String attrId;
    // 平台属性值Id baseAttrValue.id
    @Column
    String valueId;

    @Column
    String skuId;

}
