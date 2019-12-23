package com.atguigu.gmall0624.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

// 拦截器
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{

    // 进入控制器之前
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("来人了，开始接客了");
        // 获取token ，将token 放入cookie 中！token 什么时候产生？ 是不是在登录成功之后，返回newToken ，后面的数据就是token！
        // http://www.gmall.com/?newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        String token = request.getParameter("newToken");
        // 判断token
        if (token!=null){
            // 放入cookie
            CookieUtil.setCookie(request, response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        // 登录之后，继续http://list.gmall.com/list.html?catalog3Id=61 商品检索。 cookie 中是否有token？
        if (token==null){
            token = CookieUtil.getCookieValue(request,"token",false);
        }
        // 获取用户的昵称
        if (token!=null){
            // 用户昵称从token 中获取！token 中有map{userId,nickName}
            Map map = makeUserInfo(token);

            String nickName = (String) map.get("nickName");
            // 保存到作用域
            request.setAttribute("nickName",nickName);
        }
        // 主要获取用户访问控制器方法上的注解 借助handler 把它转换为请求方法
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 获取方法上的LoginRequire注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation!=null){
            // 该方法上有注解：
            // 当注解的属性值为true 时必须登录！
            // 先判断当前系统是否有用户登录！ true 登录{注解上的属性是否true，false 有影响么？没有！}
            // 调用认证方法！如果返回success ，表示已经登录，fail 没有登录。 没有登录{注解上的方法属性还是true，则必须跳转到登录页面}
            /*
             远程调用：verify  {ip，port 不相同！ 如果是a 项目调用b 项目的控制器，使用http方式调用[httpClientUtils,spring cloud, spring cloud alibaba ]
                    如果是a 项目调用b 项目的接口，使用dubbo！
             }
              */
            // http://passport.atguigu.com/verify?token=xx&salt=x
            // http://passport.atguigu.com/verify
            // 获取salt
            String salt = request.getHeader("X-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){
                // 登录成功 记录一下谁登录了。记录userId 即可！
                Map map = makeUserInfo(token);

                String userId = (String) map.get("userId");
                // 保存到作用域
                request.setAttribute("userId",userId);
                // 放行！
                return true;
            } else {
                // 认证失败！
                if (methodAnnotation.autoRedirect()){
                    // 必须登录！
                    // 访问 http://item.gmall.com/38.html  -- 跳转到 http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F38.html
                    // 获取用户访问的url 路径
                    String requestURL = request.getRequestURL().toString();
                    System.out.println(requestURL);// http://item.gmall.com/38.html
                    // http://item.gmall.com/38.html -- http%3A%2F%2Fitem.gmall.com%2F38.html
                    String encoderURL = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println(encoderURL);//http%3A%2F%2Fitem.gmall.com%2F38.html

                    // 跳转到登录
                    // request.getRequestDispatcher("").forward(request,response); // 转发
                    // http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F38.html
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encoderURL); // 重定向
                    return false;

                }
            }
        }


        // 放行拦截器：
        return true;
    }

    // 获取用户信息
    private Map makeUserInfo(String token) {
        // eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        // 中间部分：eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0
        // 解密token 有两种：一种：使用工具类 ，另一种：使用base64 编码解密
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        // 创建base64 对象
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        // 获取解密的字符串
//        String userJson = base64UrlCodec.encode(tokenUserInfo.getBytes());
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        // 字节数组转化为字符串
        String userJson = new String(bytes);
        // userJson 转换成map
        Map map = JSON.parseObject(userJson, Map.class);
        System.out.println(map);
        return map;


    }

    // 进入控制器之后，返回视图之前
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }
    // 视图渲染之后
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

}
