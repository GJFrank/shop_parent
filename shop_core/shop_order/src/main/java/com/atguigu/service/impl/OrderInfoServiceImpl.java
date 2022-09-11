package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.mapper.OrderDetailMapper;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-09
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${cancel.order.delay}")
    private Integer cancelOrderDelay;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public String generateTradeNo(String userId) {
        //生成的时候, 需要在redis中也存放一份
        String tradeNo = UUID.randomUUID().toString();
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    @Override
    public boolean compareTradeNo(String userId, String tradeNoUI) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        String TradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNoUI.equals(TradeNoRedis);
    }

    @Override
    public Long saveOrderAndDetail(OrderInfo orderInfo) {
        // 订单基本信息
        // order_status 先不管
        String outTradeNo = "atguigu" + System.currentTimeMillis();
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setTradeBody("天热玩个锤子!");
        //设置订单的过期时间
        orderInfo.setCreateTime(new Date());
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 15);
        orderInfo.setExpireTime(instance.getTime());
        //订单的进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //保存订单基本信息
        baseMapper.insert(orderInfo);
//        保存订单详情信息
        Long orderId = orderInfo.getId();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderId);
        }
        orderDetailService.saveBatch(orderDetailList);
        //发送一个延时消息, 超时自动取消订单
        rabbitTemplate.convertAndSend(MqConst.CANCEL_ORDER_EXCHANGE, MqConst.CANCEL_ORDER_ROUTE_KEY, orderId,
                correlationData -> {
                    correlationData.getMessageProperties().setDelay(cancelOrderDelay);
                    return correlationData;
                });
        return orderId;
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //2. 获得订单详情
        if(orderInfo!=null){
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(wrapper.eq(OrderDetail::getOrderId, orderId));
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }

    @Override
    public void updateOrderStatusByProcessStatus(OrderInfo orderInfo, ProcessStatus processStatus) {
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        baseMapper.updateById(orderInfo);
    }

    @Override
    public void sendMessageToWareHouse(OrderInfo orderInfo) {
        //1. 将订单状态改为 已通知仓库
        updateOrderStatusByProcessStatus(orderInfo, ProcessStatus.NOTIFIED_WARE);

        //2. 组织一个Map<String,Object> 给仓库系统
        Map<String, Object> houseData = assembleWareHouseData(orderInfo);
        String jsonData = JSON.toJSONString(houseData);
        //3. 发消息给仓库系统
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE, MqConst.DECREASE_STOCK_ROUTE_KEY, jsonData);
    }

    @Override
    public String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson) {
        //1. 获取原始订单popopo
        OrderInfo parentOrderInfo = getOrderInfo(orderId);
        OrderInfo parentOrderInfo1 = getOrderInfo(orderId);
        //参数格式转换为list  [{"wareHouseId":"1","skuIdList":["24","28"]},{"wareHouseId":"2","skuIdList":["25,30,32"]}]
        List<Map> wareHouseIdSkuIdMapList = JSON.parseArray(wareHouseIdSkuIdMapJson, Map.class);
        //多个拼接好的仓库信息
        ArrayList<Object> assembleWareHouseDataList = new ArrayList<>();

        for (Map wareHouseIdSkuIdMap : wareHouseIdSkuIdMapList) {
            String wareHouseId = (String) wareHouseIdSkuIdMap.get("wareHouseId");
            List<String> skuIdList = (List<String>) wareHouseIdSkuIdMap.get("skuIdList");

            //2. 设置子订单信息
            OrderInfo childOrderInfo = new OrderInfo();
            //copy属性
            BeanUtils.copyProperties(parentOrderInfo, childOrderInfo);
            childOrderInfo.setParentOrderId(orderId);
            childOrderInfo.setId(null);

            childOrderInfo.setWareHouseId(wareHouseId);
            //3.设置子订单详情信息, 该子订单中有哪些明细 子订单金额
            BigDecimal childTotalMoney = new BigDecimal("0");

            List<OrderDetail> childOrderDetailList = new ArrayList<>();
            List<OrderDetail> parentOrderDetailList = parentOrderInfo.getOrderDetailList();

            for (OrderDetail parentOrderDetail : parentOrderDetailList) {
                for (String skuId : skuIdList) {
                    if (Long.parseLong(skuId) == parentOrderDetail.getSkuId()) {
                        BigDecimal orderPrice = parentOrderDetail.getOrderPrice();
                        String skuNum = parentOrderDetail.getSkuNum();
                        childTotalMoney = childTotalMoney.add(orderPrice.multiply(new BigDecimal(skuNum)));
                        childOrderDetailList.add(parentOrderDetail);
                    }
                }
            }
            childOrderInfo.setTotalMoney(childTotalMoney);
            childOrderInfo.setOrderDetailList(childOrderDetailList);

            //保存子订单及其明细
            saveOrderAndDetail(childOrderInfo);
            Map<String, Object> wareHouseData = assembleWareHouseData(childOrderInfo);
            assembleWareHouseDataList.add(wareHouseData);
        }

        //4. 原始订单 task_status 改为split
        updateOrderStatusByProcessStatus(parentOrderInfo, ProcessStatus.SPLIT);
        //5. 返回信息给仓库系统
        return JSON.toJSONString(assembleWareHouseDataList);

    }

    @Override
    public String checkStockAndPrice(String userId, OrderInfo orderInfo) {
        //a.拿到用户购买的商品清单
        StringBuilder sb=new StringBuilder();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                Long skuId = orderDetail.getSkuId();
                String skuNum = orderDetail.getSkuNum();
                //b.判断每个商品库存是否足够 访问库存接口
//                String url="http://localhost:8100/hasStock?skuId="+skuId+"&num="+skuNum;
//                String result = HttpClientUtil.doGet(url);
//                //0无库存 1有库存
//                if("0".equals(result)){
//                    sb.append(orderDetail.getSkuName()+"库存不足");
//                }
                //判断价格
                BigDecimal realTimePrice = productFeignClient.getSkuPrice(skuId);
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                if(realTimePrice.compareTo(orderPrice)!=0){
                    sb.append(orderDetail.getSkuName()+"价格有变化请刷新页面");
                }
            }
        }
        return sb.toString();
    }

    private Map<String, Object> assembleWareHouseData(OrderInfo orderInfo) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("orderId", orderInfo.getId());
        dataMap.put("consignee", orderInfo.getConsignee());
        dataMap.put("consigneeTel", orderInfo.getConsigneeTel());
        dataMap.put("orderComment", orderInfo.getOrderComment());
        dataMap.put("orderBody", orderInfo.getTradeBody());
        dataMap.put("deliveryAddress", orderInfo.getDeliveryAddress());
        dataMap.put("paymentWay", 2);
        //设置一个仓库id, 给仓库拆完单后 返回值使用, 无法正常跑
        String wareHouseId = orderInfo.getWareHouseId();
        if (!StringUtils.isEmpty(wareHouseId)) {
            dataMap.put("wareId", wareHouseId);
        }
        //商品清单
        List<Map<String, Object>> orderDetailMapList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId", orderDetail.getSkuId());
            orderDetailMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailMap.put("skuName", orderDetail.getSkuName());
            orderDetailMapList.add(orderDetailMap);
        }
        dataMap.put("details", orderDetailMapList);
        return dataMap;
    }

}
