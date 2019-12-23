package com.atguigu.gmall0624.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.UserInfo;
import com.atguigu.gmall0624.passport.config.JwtUtil;
import com.atguigu.gmall0624.service.UserInfoService;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    //登录控制器
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
        //调用服务层
        UserInfo info = userInfoService.login(userInfo);

        if (info!=null){
            //String key = "shiliu";
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
           //String salt = "192.168.93.1";
            String salt = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(key, map, salt);
            return token;
        }

        return "fail";//登录失败
    }

    // 用户的认证！
    // http://passport.atguigu.com/verify?token=xx&salt=x
    @RequestMapping("verify")
    @ResponseBody
    public String verify (HttpServletRequest request){
        /*
           	a.	从url 路径上得到token ，salt
            b.	使用jwt 解密得到用户的数据{map}
            c.	获取map 中的userId 查询缓存
            d.	true:success 	false:fail
         */
        // 需要得到token,salt
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        // 调用JWT 工具类
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if (map!=null && map.size()>0){
            String userId = (String) map.get("userId");
            // 调用服务层查看缓存中是否有用户数据
            UserInfo userInfo = userInfoService.verify(userId);

            if (userInfo!=null){
                // 缓存中有数据
                return "success";
            }

        }
        return "fail";
    }
}
