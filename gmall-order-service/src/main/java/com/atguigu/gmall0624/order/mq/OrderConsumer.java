package com.atguigu.gmall0624.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.enums.ProcessStatus;
import com.atguigu.gmall0624.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        // 获取消息队列中的支付结果
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        // result为success
        if ("success".equals(result)){
            // 支付成功 修改订的状态！
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送消息给库存系统，实现减库存 传入的参数订单Id
            orderService.sendOrderStatus(orderId);
            // 更新订单的状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }
    }
    // 监听减库存结果的消息队列
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 获取消息队列中的支付结果
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");

        if ("DEDUCTED".equals(status)){
            // 减库存成功 ,将订单的状态更新为等待发货
            orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);

        }else {
            // 从其他仓库调库存！远程调用补货！
            // 预警：
        }
    }



}
