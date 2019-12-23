package com.atguigu.gmall0624.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.enums.ProcessStatus;
import com.atguigu.gmall0624.service.OrderService;
import com.atguigu.gmall0624.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @Reference
    private OrderService orderService;

    // 监听减库存结果的消息队列
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerCheckQueue(MapMessage mapMessage) throws JMSException {
        // 获取消息队列中的支付结果
        String outTradeNo = mapMessage.getString("outTradeNo");

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        // 判断是否支付成功？
        boolean result = paymentService.checkPayment(orderInfo);
        if (!result){
            // 如果没有支付，关闭订单！ 修改状态
            OrderInfo orderInfoQuery = orderService.getOrderInfo(orderInfo);
            orderService.updateOrderStatus(orderInfoQuery.getId(), ProcessStatus.CLOSED);
            System.out.println("订单已经关闭！");
        }
    }
}
