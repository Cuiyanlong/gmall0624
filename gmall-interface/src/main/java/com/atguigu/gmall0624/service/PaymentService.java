package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 更新
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    /**
     * 查询
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据orderId 退款
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 生产微信支付的Map
     * @param orderId
     * @param total_fee
     * @return
     */
    Map createNative(String orderId, String total_fee);

    /**
     *
     * @param orderId
     * @param result
     */
    void sendPaymentResult(String orderId, String result);

    /**
     *
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    /**
     * 查询支付结果
     * @param orderInfo
     * @return
     */
    boolean checkPayment(OrderInfo orderInfo);

    /**
     * 开启延迟队列发送消息
     * @param outTradeNo 因为查询是否支付成功需要的参数
     * @param delaySec 设置延迟的队列发送的时间
     */
    void closeOrderInfo(String outTradeNo,int delaySec);
}
