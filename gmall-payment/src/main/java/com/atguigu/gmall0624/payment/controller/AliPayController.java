package com.atguigu.gmall0624.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.PaymentInfo;
import com.atguigu.gmall0624.bean.enums.PaymentStatus;
import com.atguigu.gmall0624.payment.config.AlipayConfig;
import com.atguigu.gmall0624.service.OrderService;
import com.atguigu.gmall0624.service.PaymentService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.catalina.manager.Constants.CHARSET;

@Controller
public class AliPayController {

    // 想要将AlipayClient 注入到spring 容器中！
    @Autowired
    private AlipayClient alipayClient;

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    // http://payment.gmall.com/alipay/submit
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String aliPaySubmit(HttpServletRequest request, HttpServletResponse response){
        // 判断支付日志中的状态是否为已经支付，如果已经支付则return

        // 获取订单编号
        String orderId = request.getParameter("orderId");
        // 支付宝的参数与订单有关系
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        // 保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("大衣，帽子");
        paymentInfo.setCreateTime(new Date());

        paymentService.savePaymentInfo(paymentInfo);

        // 生成二维码
        // @Bean = <bean> </bean>
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址0

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",orderInfo.getTotalAmount());
        map.put("subject","大衣，帽子");
        // 封装参数
        alipayRequest.setBizContent(JSON.toJSONString(map));
//        alipayRequest.setBizContent("{" +
//                "    \"out_trade_no\":"+orderInfo.getOutTradeNo()+"," +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                "    \"total_amount\":88.88," +
//                "    \"subject\":\"Iphone6 16G\"," +
//                "    \"body\":\"Iphone6 16G\"," +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                "    \"extend_params\":{" +
//                "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                "    }"+
//                "  }");//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + CHARSET);
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        return form;
    }

    // 当支付成功之后，给用户看下订单页面 。
    @RequestMapping("alipay/callback/return")
    public String callBack(){
        // http://trade.gmall.com/trade
        return   "redirect:"+AlipayConfig.return_order_url;
    }

    // 异步回调：必须要使用内网穿透
    @RequestMapping("alipay/callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        //System.out.println("来了，兄弟！");
        // 1. 得到交易状态TRADE_SUCCESS 或 TRADE_FINISHED  说明用户支付成功！
        // 获取交易状态
        String trade_status = paramMap.get("trade_status");
        // 获取商户的订单编号
        String out_trade_no = paramMap.get("out_trade_no");

        // Map<String, String> paramsMap = ... // 将异步通知中收到的所有参数都存放到map中
        boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, CHARSET, AlipayConfig.sign_type); //调用SDK验证签名
        // 验证交易记录存在
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){

                // 如果说我们paymentInfo 中的交易状态以及是PAID，CLOSE 那么你还返回成功么？
                // 通过out_trade_no 查询PaymentInfo
                // select * from paymentInfo where out_trade_no = ?
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
                // 判断支付状态！
                if (paymentInfoQuery.getPaymentStatus()==PaymentStatus.PAID ||paymentInfoQuery.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }

                // 返回成功 ，更新交易记录状态
                // update paymentInfo set paymentStatus=PAID,callbacktime=new Date() where out_trade_no= ?
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setCallbackTime(new Date());
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(out_trade_no,paymentInfoUPD);
                // 发送消息通知订单支付成功 修改订单的状态 success  ,发送订单Id，结果

                paymentService.sendPaymentResult(paymentInfoQuery.getOrderId(),"success");

                return "success";
            }

        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        // 调用退款
        boolean flag =  paymentService.refund(orderId);
        return ""+flag;
    }

    // 控制器测试发送支付成功的消息
    // http://payment.gmall.com/sendPaymentResult?orderId=126&result=success
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }

    // 根据订单号查询
    // http://payment.gmall.com/queryPaymentResult?orderId=xxx
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(String orderId){
        // orderInfo 中只有一个属性值 那就是Id = orderId,并没有outTradeNo
        // select * from orderInfo where id = orderId
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        boolean flag = paymentService.checkPayment(orderInfo);
        return ""+flag;
    }



}
