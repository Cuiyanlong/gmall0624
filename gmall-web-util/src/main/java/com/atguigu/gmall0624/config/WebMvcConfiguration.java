package com.atguigu.gmall0624.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


// spring boot 1.x
// spring boot 2.x 实现方式
// webMvcConfiguration.xml
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Autowired(required = false)
    private AuthInterceptor authInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        /*
        <mvc:interceptor>
                <bean class="com.atguigu.gmall0624.config.AuthInterceptor">
                <mvc:mapping path="/**">
        </mvc:interceptor>
         */
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
        /*
        <mvc: interceptors>
            <mvc:interceptor>
                    <bean class="com.atguigu.gmall0624.config.AuthInterceptor">
                    <mvc:mapping path="/**">
            </mvc:interceptor>
        </mvc: interceptors>
         */
        super.addInterceptors(registry);
    }
}
