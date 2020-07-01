package com.atguigu.gmall0624.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.CartInfo;
import com.atguigu.gmall0624.bean.SkuInfo;
import com.atguigu.gmall0624.config.CookieUtil;
import com.atguigu.gmall0624.config.LoginRequire;
import com.atguigu.gmall0624.service.CartService;
import com.atguigu.gmall0624.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    // http://cart.gmall.com/addToCart
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        // 如何获取到用户Id
        String userId = (String) request.getAttribute("userId");
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        if (userId==null){
            // 未登录 在cookie保存一个临时的用户Id ,用户在未登录状态下不是第一次添加购物车！
            userId = CookieUtil.getCookieValue(request,"user-key",false);
            // 用户第一次添加购物车
            if (userId==null){
                userId= UUID.randomUUID().toString().replace("-","");
                // userId 放入到cookie！
                CookieUtil.setCookie(request,response,"user-key",userId,7*24*3600,false);
            }
        }

        // 添加购物车
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 获取skuInfo
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 如何获取到用户Id
        String userId = (String) request.getAttribute("userId");
        if (userId==null){
            // 走未登录 临时用户Id
            String userTempId  = CookieUtil.getCookieValue(request, "user-key", false);
            if (userTempId!=null){
                // 获取未登录购物车数据
                cartInfoList = cartService.getCartList(userTempId);
            }
        }else {
            // 登录情况
            String userTempId  = CookieUtil.getCookieValue(request, "user-key", false);
            List<CartInfo> cartInfoNoLoginList = new ArrayList<>();
            if (userTempId!=null){
                // 获取未登录购物车数据
                cartInfoNoLoginList = cartService.getCartList(userTempId);
                // 未登录数据不为空！
                if (cartInfoNoLoginList!=null && cartInfoNoLoginList.size()>0){
                    // 未登录+登录=合并
                    cartInfoList = cartService.mergeToCartList(cartInfoNoLoginList,userId);
                    // 删除未登录数据
                    cartService.deleteCartList(userTempId);
                }
            }
            // 如果临时用户id 为空 或者未登录购物车集合中没有数据
            if (userTempId==null || (cartInfoNoLoginList==null || cartInfoNoLoginList.size()==0)){
                cartInfoList = cartService.getCartList(userId);
            }

        }

        // 保存到作用域
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request){
        // 更新购物车商品的状态ischecked
        // 得到前台页面传递的参数
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        // 未登录
        if (userId==null){
            // 获取未登录的临时用户Id
            userId = CookieUtil.getCookieValue(request, "user-key", false);
    }
        // 调用服务层方法
        cartService.checkCart(skuId,userId,isChecked);
    }

    // 去结算  toTrade去结算，
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request){
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        // 重定向到订单页面
        // 登录，未登录都可以添加去结算！
        // 登录，可以勾选商品，未登录能勾选商品
        // 获取未登录购物车数据
        String userTempId = CookieUtil.getCookieValue(request, "user-key", false);

        if (StringUtils.isNotEmpty(userTempId)){
            // 根据临时userId 获取购物车数据
            List<CartInfo> cartInfoNoLoginList = cartService.getCartList(userTempId);
            // 以未登录为基准进行合并
            if (cartInfoNoLoginList!=null && cartInfoNoLoginList.size()>0){
                // 调用合并方法
                cartService.mergeToCartList(cartInfoNoLoginList,userId);
                // 删除未登录购物车数据
                cartService.deleteCartList(userTempId);
            }
        }
        return "redirect://trade.gmall.com/trade";
    }


}
