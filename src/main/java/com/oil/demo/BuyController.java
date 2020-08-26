package com.oil.demo;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/buy")
public class BuyController {

    @Value("${app.merchantId}")
    private String merchantId;

    @Value("${app.key}")
    private String key;

    @Value("${app.api}")
    private String api;

    @Value("${app.callback}")
    private String callback;

    private String sign(Map<String, Object> params){

        List<String> strs = params.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey()+":"+e.getValue()).collect(Collectors.toList());
        Collections.sort(strs);
        String singText = StringUtils.join(strs, '&');
        System.out.println("signText:" + singText + key);
        return new MD5().digestHex(singText + key);
    }

    @RequestMapping("callback")
    public void callback(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        System.out.println("=========回调消息开始===========");
        System.out.println(JSON.toJSONString(req.getParameterMap()));
        System.out.println("=========回调消息结束===========");
        resp.getWriter().write("success");
    }


    @PostMapping("card")
    @ResponseBody
    public String card(String address, Integer amount,  String cardType, String mobile, String userName,String city){
        Map<String, Object> data = new HashMap(){{
            put("city", city);
            put("address", address);
            put("amount", amount);
            put("cardType", cardType);
            put("mobile", mobile);
            put("userName", userName);
            put("time", System.currentTimeMillis() / 1000);

            //附言，用于回调业务处理
            put("attach", "card" + UUID.randomUUID());
            put("callBackUrl", callback);
            put("merchantId", merchantId);
        }};

        data.put("sign", sign(data));
        System.out.println("sign:" + data.get("sign"));

        HttpRequest httpRequest = HttpRequest.post(api+"/card-orders/buy").charset("UTF-8");
        HttpResponse httpResponse = httpRequest
                .form(data)
                .execute(false);
        return httpResponse.body();
    }

    @PostMapping("oil")
    @ResponseBody
    public String oil(Integer cardMoney, String cardNo){
        Map<String, Object> data = new HashMap(){{
            put("cardMoney", cardMoney);
            put("cardNo", cardNo);
            put("time", System.currentTimeMillis() / 1000);

            //附言，用于回调业务处理
            put("attach", "oil" + UUID.randomUUID());
            put("callBackUrl", callback);
            put("merchantId", merchantId);
        }};

        data.put("sign", sign(data));
        System.out.println("sign:" + data.get("sign"));

        HttpRequest httpRequest = HttpRequest.post(api+"/orders/buy").charset("UTF-8");
        HttpResponse httpResponse = httpRequest
                .form(data)
                .execute(false);
        return httpResponse.body();
    }
}
