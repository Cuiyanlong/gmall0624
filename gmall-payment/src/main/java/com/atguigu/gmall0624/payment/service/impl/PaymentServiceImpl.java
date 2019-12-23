package com.atguigu.gmall0624.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.PaymentInfo;
import com.atguigu.gmall0624.bean.enums.PaymentStatus;
import com.atguigu.gmall0624.config.ActiveMQUtil;
import com.atguigu.gmall0624.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0624.service.PaymentService;
import com.atguigu.gmall0624.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;


    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public boolean refund(String orderId) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        // 通过orderId 查询PaymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfoQuery = getPaymentInfo(paymentInfo);
        // 声明一个map
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        map.put("refund_amount",paymentInfoQuery.getTotalAmount());
        map.put("refund_reason","不暖和");

        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 退款之后，交易状态！
            PaymentInfo paymentInfoUpd = new PaymentInfo();
            paymentInfoUpd.setPaymentStatus(PaymentStatus.ClOSED);
            updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUpd);
            paymentInfoUpd.setCallbackTime(new Date());
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Map createNative(String orderId, String total_fee) {
        /*
        1.  传递参数
        2.  发送数据给接口
        3.  获取结果
         */
        // 声明一个map集合
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("appid",appid);
        paramMap.put("mch_id",partner);
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        // paramMap.put("sign","");
        paramMap.put("body","买大衣");
        paramMap.put("out_trade_no",orderId);
        paramMap.put("total_fee",total_fee);
        paramMap.put("spbill_create_ip","127.0.0.1");
        paramMap.put("notify_url","http://guli.free.idcfengye.com/wx/callback/notify");
        paramMap.put("trade_type","NATIVE");

        // 将map 转换为xml 发送给接口
        try {
            String xmlParam = WXPayUtil.generateSignedXml(paramMap, partnerkey);

            // 接口路径：https://api.mch.weixin.qq.com/pay/unifiedorder
            // 远程调用
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            httpClient.post();

            // 获取返回的结果：
            String result = httpClient.getContent();
            // xmlToMap 将结果转换成map
            Map<String, String> resultMap  = WXPayUtil.xmlToMap(result);

            // 声明一个自己需要的map
            HashMap<Object, Object> map = new HashMap<>();
            map.put("code_url",resultMap.get("code_url"));
            map.put("total_fee",total_fee);
            map.put("out_trade_no",orderId);

            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void sendPaymentResult(String orderId, String result) {
        // 发送消息
        Connection connection = activeMQUtil.getConnection();
        // 打开连接
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建 提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",orderId);
            activeMQMapMessage.setString("result",result);
            producer.send(activeMQMapMessage);

            // 必须提交
            session.commit();

            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        // 发送消息
        Connection connection = activeMQUtil.getConnection();
        // 打开连接
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建 提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);
            producer.send(activeMQMapMessage);

            // 必须提交
            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkPayment(OrderInfo orderInfo) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        if (orderInfo==null){
            return false;
        }
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 根据trade_status 判断
            String tradeStatus = response.getTradeStatus();
            if ("TRADE_SUCCESS".equals(tradeStatus)|| "TRADE_FINISHED".equals(tradeStatus)){
                System.out.println("交易成功");
                return true;
            }

        } else {
            System.out.println("调用失败，交易失败！");
            return false;
        }
        return false;
    }

    @Override
    public void closeOrderInfo(String outTradeNo, int delaySec) {
        // 打开链接
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();

            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(payment_result_check_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);

            // 设置延迟的时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            producer.send(activeMQMapMessage);

            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
