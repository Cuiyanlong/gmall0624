package com.atguigu.gmall0624.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring 如何整合redis
 * <bean class="jedis" class="">
 *   <property name="host" value=""></property>
 * </>
 */

@Configuration //xxx.xml
public class RedisConfig {
    //实现软编码:@Value表示从application中获取数据
    //：disable表示如果从配置文件中没有找到对应的数据则给一个默认值
    @Value("${spring.redis.host:disable}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;

    //将host，port和timeout给initJedisPOOL方法使用
    //@Bean 表示一个bean标签RedisUtil注入到spring容器
    @Bean
    public RedistUtil getRedistUtil(){
        //配置文件中根本没有host
        if ("disabled".equals(host)){
            return null;
        }
        RedistUtil redistUtil = new RedistUtil();
        redistUtil.initJedisPool(host,port,timeOut);
        return redistUtil;
    }



}
