package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-12
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public String createQrCode(Long orderId) throws AlipayApiException {
        //1. 根据订单id查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfoByOrderId(orderId);
        // 保存支付表信息表
        savePaymentInfo(orderInfo);

        //支付宝交易请求声明
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 支付成功之后的异步通知
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        // 支付成功之后的同步通知
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("total_amount", orderInfo.getTotalMoney());
        bizContent.put("subject", "测试商品0812jqw");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
            //这个body就是页面
            String alipayHtml = response.getBody();
            return alipayHtml;
        } else {
            System.out.println("调用失败");
        }
        return null;
    }

    private void savePaymentInfo(OrderInfo orderInfo) {
        //1.判断支付表单里面是否有添加过该记录
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderInfo.getId());
        wrapper.eq("payment_type", PaymentType.ALIPAY.name());
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            //已存在, 直接返回
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId() + "");
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        baseMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfoByOutTradeNo(String outTradeNo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no", outTradeNo);
        wrapper.eq("payment_type", PaymentType.ALIPAY.name());

        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updatePaymentInfo(Map<String, String> aliPayParam) {

        // 1. 获取out_trade_no, 以获得paymentInfo
        String outTradeNo = aliPayParam.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentInfoByOutTradeNo(outTradeNo);
        //2. 修改支付表信息
        //2.1  支付状态       --已支付
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        //2.2 回调时间
        paymentInfo.setCallbackTime(new Date());
        //2.3 整个支付传递的参数信息
        paymentInfo.setCallbackContent(aliPayParam.toString());
        //2.4 保存支付宝的交易号
        paymentInfo.setTradeNo(aliPayParam.get("trade_no"));
        //2.5 更新payment
        baseMapper.updateById(paymentInfo);
        //2.5 发消息给shop-order 修改订单状态  Long orderId
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE, MqConst.PAY_ORDER_ROUTE_KEY, paymentInfo.getOrderId());

    }
}
