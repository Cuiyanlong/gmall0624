package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 使用userId 作为key ，将流水号保存到缓存中
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     *  比较流水号
     * @param userId 获取缓存的流水号
     * @param tradeCodeNo 直接获取页面的流水号
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 删除流水号
     * @param userId
     */
    void deleteTradeCode(String userId);

    /**
     * 验证库存
     * @param skuId 商品id
     * @param skuNum 商品数量
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据订单Id 查询订单对象
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 更新订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 通知仓库系统
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询
     * @param orderInfo
     * @return
     */
    OrderInfo getOrderInfo(OrderInfo orderInfo);

    /**
     * 将orderInfo 转换为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
