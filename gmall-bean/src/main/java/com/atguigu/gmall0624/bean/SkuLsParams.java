package com.atguigu.gmall0624.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkuLsParams implements Serializable {
    //关键字
    String  keyword;
    //三级分类id
    String catalog3Id;
    //平台属性id
    String[] valueId;
    //当前页
    int pageNo=1;
    //每页显示条数
    int pageSize=20;
}
