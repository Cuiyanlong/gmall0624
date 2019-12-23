package com.atguigu.gmall0624.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.*;
import com.atguigu.gmall0624.config.LoginRequire;
import com.atguigu.gmall0624.service.*;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class OrderController {

    //  select * from userAddress where userId = ?

    //    @Autowired
    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @Reference
    private PaymentService paymentService;

    @RequestMapping("trade")
    //@ResponseBody // 第一个作用：返回json 字符串。第二个作用：将控制器中的数据直接输入到一个空白页！
    @LoginRequire
    public String trade(HttpServletRequest request){
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        // 得到用户的收货地址列表
        // return userInfoService.findUserAddressListByUserId(userId);
        List<UserAddress> userAddressList = userInfoService.findUserAddressListByUserId(userId);

        // 获取购物车中的数据：
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);

        // 保存订单明细数据，订单明细数据来自于购物车的!
        ArrayList<OrderDetail> detailsList = new ArrayList<>();
        if (cartInfoList!=null && cartInfoList.size()>0){
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                // 添加到订单明细集合中
                detailsList.add(orderDetail);

            }
        }
        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailsList);
        // 调用方法
        orderInfo.sumTotalAmount();

        // 保存作用域，给页面渲染
        request.setAttribute("detailsList",detailsList);

        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        request.setAttribute("userAddressList",userAddressList);
        // 将流水号保存到后台
        String tradeNo = orderService.getTradeNo(userId);

        request.setAttribute("tradeNo",tradeNo);
        return "trade";
    }
    // http://trade.gmall.com/submitOrder
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        // 获取页面提交的流水号
        String tradeNo = request.getParameter("tradeNo");

        // 支付使用
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);

        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        // 调用比较方法
        boolean result = orderService.checkTradeCode(userId, tradeNo);
        // 表示比较失败！
        if (!result){
            request.setAttribute("errMsg","不能重复提交订单！");
            return "tradeFail";
        }
        // 删除流水号
        orderService.deleteTradeCode(userId);

        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                // 调用库存接库
                boolean flag = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
                // flag =true 表示验证通过！
                if (!flag){
                    request.setAttribute("errMsg",orderDetail.getSkuName()+"库存不足,请重新下单！");
                    return "tradeFail";
                }

                // 验证价格：
                SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());

                // skuInfo.getPrice(); orderDetail.getOrderPrice();
                // 价格有变动
                if (skuInfo.getPrice().compareTo(orderDetail.getOrderPrice())!=0){
                    // 查询最新价格放入缓存！
                    cartService.loadCartCache(userId);
                }
                //  用户的身份  优惠券  满减  总价 - 10 -30 -50  = 订单的真实价格
            }
        }
        // 保存订单
        String orderId = orderService.saveOrder(orderInfo);
        // 开启延迟队列：时间{根据商品过期时间来定!}
        paymentService.closeOrderInfo(orderInfo.getOutTradeNo(),60*60);
        //  支付页面
        return  "redirect://payment.gmall.com/index?orderId="+orderId;
    }
    // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 返回的子订单的集合字符串Json
        // wareSkuMap = [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId,wareSkuMap);
        // 声明一个集合来存储map
        ArrayList<Map> mapArrayList = new ArrayList<>();
        // 将每个orderInfo 变成map ，map 中就是子订单
        for (OrderInfo orderInfo : subOrderInfoList) {

            Map map = orderService.initWareOrder(orderInfo);

            mapArrayList.add(map);

        }
        return JSON.toJSONString(mapArrayList);

    }
}
