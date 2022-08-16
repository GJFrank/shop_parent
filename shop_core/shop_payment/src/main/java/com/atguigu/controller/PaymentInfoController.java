package com.atguigu.controller;


import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.config.AlipayConfig;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author GodWei
 * @since 2022-08-12
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {

    @Autowired
    private PaymentInfoService paymentInfoService;

    //1. 返回支付宝二维码页面信息 返回的是一个html
    // http://api.gmall.com/payment/createQrCode/{orderId}(orderId=${orderInfo.id}
    @RequestMapping("createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId) throws AlipayApiException {
        return paymentInfoService.createQrCode(orderId);
    }

    //异步通知 验签
    // 2.支付成功之后 支付宝调用我们的地址 http://enjoy6288.free.idcfengye.com/payment/async/notify
    @PostMapping("async/notify")
    public String asyncNotify(@RequestParam Map<String, String> aliPayParam) throws AlipayApiException {
        System.out.println(aliPayParam);
        //1. 获取支付宝验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(aliPayParam, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);//调用SDK验证签名
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //处理商户自身业务结果
            String trade_status = aliPayParam.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                String outTradeNo = aliPayParam.get("out_trade_no");
                // 查找对应的paymentInfo 支付信息表
                PaymentInfo paymentInfo = paymentInfoService.getPaymentInfoByOutTradeNo(outTradeNo);
                String paymentStatus = paymentInfo.getPaymentStatus();

                //如果paymentStatus已经支付 或者 已经关闭, 则不需要再执行了
                if (paymentStatus.equals(PaymentStatus.PAID.name()) || paymentStatus.equals(PaymentStatus.ClOSED)) {
                    return "success";
                }
                //修改支付表信息
                paymentInfoService.updatePaymentInfo(aliPayParam);
            }
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";

    }

}

