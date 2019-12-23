package com.atguigu.gmall0624.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.SkuInfo;
import com.atguigu.gmall0624.bean.SkuSaleAttrValue;
import com.atguigu.gmall0624.bean.SpuSaleAttr;
import com.atguigu.gmall0624.config.LoginRequire;
import com.atguigu.gmall0624.service.ListService;
import com.atguigu.gmall0624.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable String skuId, HttpServletRequest request){

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //通过spuId，skuid查询销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        //销售属性值切换
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        //{"122|128":37,"122|126":38}json字符串
        //声明一个map  | map.put("122|126":37)
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            //拼接：下一次循环的时候的skuId相同时拼接，不同不拼接】
            //循环最后停止拼接
            for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
                if (key.length()>0){
                    key+="|";
                }
                key+=skuSaleAttrValue.getSaleAttrValueId();

                if ((i+1)==skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                    map.put(key,skuSaleAttrValue.getSkuId());
                    key="";
                }
            }
        }

        //将map转换为json字符串
        String valuesSkuJson = JSON.toJSONString(map);

        //保存JSON字符串
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        //保存skuInfo给页面渲染
        request.setAttribute("skuInfo",skuInfo);

        //调用热度排名
        listService.incrHotScore(skuId);
        return "item";
    }
}
