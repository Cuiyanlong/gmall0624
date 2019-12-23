package com.atguigu.gmall0624.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.OrderDetail;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.enums.OrderStatus;
import com.atguigu.gmall0624.bean.enums.ProcessStatus;
import com.atguigu.gmall0624.config.ActiveMQUtil;
import com.atguigu.gmall0624.config.RedistUtil;
import com.atguigu.gmall0624.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0624.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0624.service.OrderService;
import com.atguigu.gmall0624.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.redisson.Redisson;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    // 调用mapper
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedistUtil redistUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        // 两张表 orderInfo ,orderDetail
        // 总金额，订单状态，[用户Id]，第三方交易变化，创建时间，过期时间，进程状态
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        // 支付使用
//        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
//        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 过期时间：+1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);

        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        orderInfoMapper.insertSelective(orderInfo);

        // 保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setId(null);
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {

        // 生产流水号
        String outTradeNo = UUID.randomUUID().toString().replace("-","");
        // 保存到缓存
        Jedis jedis = redistUtil.getJedis();
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 使用String 数据类型
        jedis.set(tradeNoKey,outTradeNo);

        // jedis 关闭
        jedis.close();
        return outTradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 获取缓存的流水号
        Jedis jedis = redistUtil.getJedis();
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 获取数据
        String redisTradeNo = jedis.get(tradeNoKey);

        jedis.close();
        return tradeCodeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeCode(String userId) {
        // 获取缓存的流水号
        Jedis jedis = redistUtil.getJedis();
        // 定义一个key
        String tradeNoKey = "user:"+userId+":tradeCode";

        jedis.del(tradeNoKey);

        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {

        // http://www.gware.com/hasStock?skuId=10221&num=2
        String res = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
//        if ("1".equals(res)){
//            return true;
//        }
        return "1".equals(res);
    }


    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        // 根据订单id 查询订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }

    @Override
    public void sendOrderStatus(String orderId) {
        // 获取连接
        Connection connection = activeMQUtil.getConnection();
        // 发送的json 字符串 字符串只是orderInfo的部分属性
        String wareJson = initWareOrder(orderId);
        // 打开连接
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            // 消息队列名称ORDER_RESULT_QUEUE
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            // 创建发送消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(wareJson);
            // 发送消息
            producer.send(activeMQTextMessage);

            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public OrderInfo getOrderInfo(OrderInfo orderInfo) {
        // 单独只针对outTradeNo,id 查询
        return orderInfoMapper.selectOne(orderInfo);
    }

    /**
     * 根据orderId 查询订单对象的部分属性数据
     * @param orderId
     * @return json字符串
     */
    private String initWareOrder(String orderId) {
        // 先得到orderInfo
        // 有orderInfo 的属性，还有orderDetail集合的属性
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将 orderInfo 中的部分属性放到map 集合中，然后将map 转换为json 字符串！
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","买大衣");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId()); // 下午使用，拆单
        /*
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        // 声明一个集合来存储map
        ArrayList<Map> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                HashMap<String, Object> detailMap = new HashMap<>();
                detailMap.put("skuId",orderDetail.getSkuId());
                detailMap.put("skuNum",orderDetail.getSkuNum());
                detailMap.put("skuName",orderDetail.getSkuName());

                mapArrayList.add(detailMap);
            }
        }
        map.put("details",mapArrayList);
        return map;
    }

    /**
     *  根据orderId ，wareSkuMap 求出子订单的集合
     * @param orderId
     * @param wareSkuMap
     * @return 子订单的集合
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<OrderInfo>();
        /*
        1.  找出谁需要拆单，找出原始订单
        2.  wareSkuMap [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}] 转换成能操作的对象
        3.  拆单：子订单生成 给子订单进行赋值
        4.  保存子订单
        5.  将子订单添加到集合
        6.  原来的订单的？更新状态！
        demo:
            原始订单：
                37 1号
                38 2号
                39 2号
            支付的时候：
                子订单
                子订单1: 37 应该是单独的子订单
                子订单2: 38，39 另一个子订单
         */

        OrderInfo orderInfoOrigin  = getOrderInfo(orderId);

        // 将数据转换为list的map
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (maps!=null && maps.size()>0){
            // 循环遍历 map={"wareId":"1","skuIds":["2","10"]}
            for (Map map : maps) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 仓库所对应的商品Id集合
                List<String> skuIds = (List<String>) map.get("skuIds");
                // 新的子订单
                OrderInfo subOrderInfo = new OrderInfo();

                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);

                // 比如说Id,不能重复
                subOrderInfo.setId(null);
                // 父id
                subOrderInfo.setParentOrderId(orderId);
                // 赋值仓库Id
                subOrderInfo.setWareId(wareId);
                // 价格重新计算 ：根据订单明细
                // 声明一个子订单的明细集合
                ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
                // 获取子订的明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    // 与仓库中的商品Id 进行匹配
                    for (String skuId : skuIds) {
                        if (skuId.equals(orderDetail.getSkuId())){
                            // 将子订单明细添加到子订的集合中
                            orderDetail.setId(null);
                            detailArrayList.add(orderDetail);
                        }
                    }
                }
                // 子订单明细集合付给子订单
                subOrderInfo.setOrderDetailList(detailArrayList);

                // 计算总价格
                subOrderInfo.sumTotalAmount();

                // 保存子订单
                saveOrder(subOrderInfo);

                subOrderInfoList.add(subOrderInfo);
            }
        }
        // 更改订单状态！
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }
}
