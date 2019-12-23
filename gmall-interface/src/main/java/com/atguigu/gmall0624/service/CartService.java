package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.CartInfo;

import java.util.List;

public interface CartService {

    // 接口 添加购物车数据

    /**
     * 添加购物车
     * @param skuId 商品Id
     * @param userId 用户Id
     * @param skuNum 商品数量
     */
    void addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 根据用户Id 查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId);

    /**
     * 删除未登录购物车
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 更新选中状态！
     * @param skuId
     * @param userId
     * @param isChecked
     */
    void checkCart(String skuId, String userId, String isChecked);

    /**
     * 根据用户Id 查询用户地址列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据userId查询实时价格
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
