package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * projectName: shop_parent
 *
 * @author: GOD伟
 * time: 2022/8/7 17:18 周日
 * description:全局拦截器
 */
@Component
public class AccessFilter implements GlobalFilter {

    @Value("${filter.whiteList}")
    public String filterWhiteList;

    @Autowired
    private RedisTemplate redisTemplate;

    //匹配路径的对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * @param exchange 服务网络交换机,用于存储请求和响应消息, 不可变的实例
     * @param chain    网关过滤链表, 用于链式调用
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        //未登录情况下获得临时userId
        String userTempId = getUserTempId(request);
        //登录成功之后可以获得userId
        String userId = getUserId(request);

        //内部接口, 不允许直接访问
        if (antPathMatcher.match("/sku/**", path)) {
            //写信息给浏览器
            return writeDataToBrowser(exchange);
        }

        //对于白名单页面, 需要在未登录的的条件下才进入login页面
        for (String filterWhite : filterWhiteList.split(",")) {
            if (path.indexOf(filterWhite) != -1 && StringUtils.isEmpty(userId)) {
                //让他跳转到登录页面
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://passport.gmall.com/login.html?originalUrl=" + request.getURI());
                return response.setComplete();
            }
        }

        //将用户信息保存在header中,tempUserId  传给shop-web那边的request
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            if (!StringUtils.isEmpty(userId)) {
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)) {
                request.mutate().header("userTempId", userTempId).build();
            }
            //放开拦截器, 让下游继续执行,(传递了参数)
            return chain.filter(exchange.mutate().request(request).build());
        }

        //如果不想拦截 就放行
        return chain.filter(exchange);
    }

    private Mono<Void> writeDataToBrowser(ServerWebExchange exchange) {
        //通过response将信息返回给浏览器
        ServerHttpResponse response = exchange.getResponse();
        //设置给浏览器的数据格式 json
        response.getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        //写的数据是什么
        RetVal<Object> retVal = RetVal.build(null, RetValCodeEnum.NO_PERMISSION);
        //把数据转换为json
        byte[] bytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(dataBuffer));
    }

    private String getUserId(ServerHttpRequest request) {
        //获取登录成功之后的token
        String token = "";
        List<String> headerValueList = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(headerValueList)) {
            token = headerValueList.get(0);
        } else {
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (cookie != null) {
                token = cookie.getValue();
            }
        }
        //2. 通过token获得用户的id
        if (!StringUtils.isEmpty(token)) {
            String userKey = "user:login:" + token;
            //此时redis里面有一份数据, 但是取不到?
            JSONObject loginUserInfoJson = (JSONObject) redisTemplate.opsForValue().get(userKey);
            if (loginUserInfoJson != null) {
                //拿到登录时的ip
                String latestLoginIp = loginUserInfoJson.getString("loginIp");
                //拿到当前的ip //todo 为什么Gateway
                String gatewayIpAddress = IpUtil.getGatewayIpAddress(request);
                if (gatewayIpAddress.equals(latestLoginIp)) {
                    //两者ip相等,  才可以获取里面的userId
                    return loginUserInfoJson.getString("userId");
                }
            }
        }
        return null;
    }

    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        List<String> headerValueList = request.getHeaders().get("userTempId");
        if (!CollectionUtils.isEmpty(headerValueList)) {
            userTempId = headerValueList.get(0);
        } else {
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if (cookie != null) {
                userTempId = cookie.getValue();
            }
        }
        return userTempId;
    }


}
