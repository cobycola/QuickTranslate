package com.zionysus.Service.Impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zionysus.DTO.TranslateRequest;
import com.zionysus.Service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static cn.hutool.crypto.SecureUtil.md5;

@Slf4j
@Service("baiduTranslateService")
public class BaiduTranslateServiceImpl implements TranslateService {

    @Value("${baidu.appid}")
    private String appid;

    @Value("${baidu.key}")
    private String key;

    @Value("${baidu.endpoint}")
    private String endpoint;


    @Override
    public String translate(TranslateRequest request) {
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(appid + request.getText() + salt + key);

        Map<String, Object> params = new HashMap<>();
        params.put("q", request.getText());
        params.put("from", request.getFrom());
        params.put("to", request.getTo());
        params.put("appid", appid);
        params.put("salt", salt);
        params.put("sign", sign);
        String requestUrl = endpoint + "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");
        log.info("调用百度翻译API，URL: {}", requestUrl);
        try {
            // 发送GET请求并获取响应
            HttpResponse response = HttpRequest.get(endpoint)
                    .form(params)
                    .execute();

            if (!response.isOk()) {
                return "Error: " + response.getStatus();
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.containsKey("error_code")) {
                return "Error: " + json.getStr("error_msg");
            }

            JSONObject result = json.getJSONArray("trans_result").getJSONObject(0);
            return result.getStr("dst");
        } catch (Exception e) {
            return "Translation failed：" + e.getMessage();
        }
    }
}