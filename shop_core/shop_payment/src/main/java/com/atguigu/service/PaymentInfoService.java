package com.atguigu.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author GodWei
 * @since 2022-08-12
 */
public interface PaymentInfoService extends IService<PaymentInfo> {


    String createQrCode(Long orderId) throws AlipayApiException;

    PaymentInfo getPaymentInfoByOutTradeNo(String outTradeNo);

    void updatePaymentInfo(Map<String, String> aliPayParam);
}
