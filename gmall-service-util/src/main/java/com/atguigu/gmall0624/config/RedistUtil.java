package com.atguigu.gmall0624.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedistUtil {
    /**
     * 1, 创建连接池
     * 2，初始化连接池
     * 3，从连接池获取Jedis
     */
    private JedisPool jedisPool;

    //String host = "192.168.93.225"
    //初始化
    public void initJedisPool(String host,int port,int timeout){
        //创建配置连接池的参数类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置连接池最大核心数
        jedisPoolConfig.setMaxTotal(200);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //最少剩余数
        jedisPoolConfig.setMinIdle(10);
        //排队等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        //设置当用户取到jedis时，做自检看当前获取到的jedis是否可以使用
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout);
    }

    //获取Jedis
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}
