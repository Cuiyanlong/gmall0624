package com.atguigu.gmall0624.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alipay.api.AlipayClient;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.config.LoginRequire;
import com.atguigu.gmall0624.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){

        //获取订单id
        String orderId = request.getParameter("orderId");
        //根据orderId查询金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //orderInfo.sumTotalAmount();
        //保存数据
        request.setAttribute("orderId",orderId);
        request.setAttribute("orderInfo",orderInfo.getTotalAmount());
        return "index";
    }
}
