package com.atguigu.gmall0624.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.service.PaymentService;
import com.atguigu.gmall0624.util.IdWorker;
import com.atguigu.gmall0624.util.StreamUtil;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WxController {

    @Reference
    private PaymentService paymentService;

    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @RequestMapping("wx/submit")
    @ResponseBody
    public Map WxSubmit(){
        //该控制器
        //通过订单号生成二维码
        //生成唯一订单编号
        IdWorker idWorker = new IdWorker();
        long orderId = idWorker.nextId();
        Map map = paymentService.createNative(orderId+"","1");
        System.out.println(map.get("code_url"));
        return map;
    }

    @RequestMapping("/wx/callback/notify")
    @ResponseBody
    public String wxNotify(HttpServletRequest request, HttpServletResponse response ) throws Exception {
        //  0 获得值
        ServletInputStream inputStream = request.getInputStream();
        String xmlString = StreamUtil.inputStream2String(inputStream,"utf-8");

        // 1 验签
        if( WXPayUtil.isSignatureValid(xmlString,partnerkey )){
            //2 判断状态
            Map<String, String> paramMap = WXPayUtil.xmlToMap(xmlString);
            String result_code = paramMap.get("result_code");
            if(result_code!=null&&result_code.equals("SUCCESS")){
                // 3 更新支付状态  包发送 消息给订单

                //  4  准备返回值 xml
                HashMap<String, String> returnMap = new HashMap<>();
                returnMap.put("return_code","SUCCESS");
                returnMap.put("return_msg","OK");

                String returnXml = WXPayUtil.mapToXml(returnMap);
                response.setContentType("text/xml");
                System.out.println("交易编号："+paramMap.get("out_trade_no")+"支付成功！");
                return  returnXml;

            }else{
                System.out.println(paramMap.get("return_code")+"---"+paramMap.get("return_msg"));
            }
        }
        return  null;
    }

}
