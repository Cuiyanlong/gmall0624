package com.atguigu.gmall0624.order.task;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 当条件，时间满足的情况下触发
@EnableScheduling
@Component
public class OrderTask {
    // 每分钟的第五秒开始执行
    @Scheduled(cron = "5 * * * * ?")
    public void test01(){
        System.out.println("开始初始化数据。。。。。。");
    }
    // 每隔五秒执行一次
    @Scheduled(cron = "0/5 * * * * ?")
    public void test02(){
        System.out.println("夜深人静的时候，开始。。。。。。处理数据统计今天的营业额");
        // 业务逻辑
    }
}
