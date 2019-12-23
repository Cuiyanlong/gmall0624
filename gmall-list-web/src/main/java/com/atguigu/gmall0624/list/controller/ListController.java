package com.atguigu.gmall0624.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.*;
import com.atguigu.gmall0624.service.ListService;
import com.atguigu.gmall0624.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
//    @ResponseBody
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        // 设置分页数据：每页显示的条数
        skuLsParams.setPageSize(3);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        // 获取到平台属性值Id 集合 (171,81,120,167,82,83)
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();

        // 调用方法将Id 集合传入
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        // 如何保存用户查询的条件
        String urlParam = makeUrlParam(skuLsParams); // 如果当前对象 skuLsParams 的三级分类Id 不为空，说明用户走的三级分类Id ，keyword 不为空，则走的keyword

        // 声明一个面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        // 点击平台属性值过滤时，平台属性消失 ---- itar，iter，itco ?
        if (baseAttrInfoList!=null && baseAttrInfoList.size()>0){

            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                // 得到平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                // 循环遍历
                // 说明用户点击平台属性值过滤
                if (skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    // url 后面的valueId
                    for (String valueId : skuLsParams.getValueId()) {
                        // 从mysql， 查询出来 valueId
                        for (BaseAttrValue baseAttrValue : attrValueList) {
                            // valueId 是否相等
                            if (valueId.equals(baseAttrValue.getId())){
                                // 移除当前数据
                                iterator.remove();

                                // 声明一个平台属性值对象
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();

                                // 组成面包屑： 平台属性名称：平台属性值名称
                                // baseAttrValueed.valueName 做成了面包屑
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                                // 制作新的urlParam
                                String newUrlParam = makeUrlParam(skuLsParams, valueId);
                                // 将最新的参数付给当前变量
                                baseAttrValueed.setUrlParam(newUrlParam);

                                baseAttrValueArrayList.add(baseAttrValueed);

                            }
                        }
                    }
                }
            }
        }
        // 设置分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        // 面包屑显示
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);

        // 保存关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());

        request.setAttribute("urlParam",urlParam);
        // 保存到作用域
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        // 存储商品数据：
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        return "list";

    }
    // 制作参数

    /**
     *
     * @param skuLsParams 表示用户url 中输入的查询参数条件
     * @param excludeValueIds 表示用户点击面包屑时传递过来的平台属性值Id
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        // 用户走的是全文检索
        // http://list.gmall.com/list.html?keyword=小米
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // urlParam = keyword=小米
            // 'list.html?'+${urlParam} == http://list.gmall.com/list.html?keyword=小米
            urlParam+="keyword="+skuLsParams.getKeyword();
        }

        // 用户走的是三级分类Id
        // http://list.gmall.com/list.html?catalog3Id=61
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        // 判断是否有平台属性值Id
        // 用户用户通过三级分类Id 查询，第二步平台属性值过滤
        // http://list.gmall.com/list.html?catalog3Id=61&valueId=82
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环
            for (String valueId : skuLsParams.getValueId()) {
                // 用户点击时的平台属性值Id
                if (excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (valueId.equals(excludeValueId)){
                        // 使用什么停止当前次数的拼接！
                        // break，continue ，return
                        continue;
                    }
                }

                // 有平台属性值Id是要拼接 &
                urlParam+="&valueId="+valueId;
            }
        }

        return urlParam;
    }
}
