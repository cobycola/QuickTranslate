package com.zionysus.Service.Impl;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zionysus.DTO.TranslateRequest;
import com.zionysus.Service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
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


    private Map<String, Object> buildParams(TranslateRequest request) {
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(appid + request.getText() + salt + key);
        Map<String, Object> params = new HashMap<>();
        params.put("q", request.getText());
        params.put("from", request.getFrom());
        params.put("to", request.getTo());
        params.put("appid", appid);
        params.put("salt", salt);
        params.put("sign", sign);
        return params;
    }

    @Override
    public String translateByGet(TranslateRequest request) {
        Map<String, Object> params = buildParams(request);
        UrlBuilder urlBuilder = UrlBuilder.of(endpoint, Charset.forName("UTF-8"));
        params.forEach((k, v) -> urlBuilder.addQuery(k, String.valueOf(v)));
        String requestUrl = urlBuilder.build();
        log.info("调用百度API，URL: {}", requestUrl);
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

    @Override
    public String translateByPost(TranslateRequest request) {
        if (request.getText() == null || request.getText().length() > 1000) {
            return "错误：文本长度超过百度API单次1000字符限制，请缩短文本。当前长度："
                    + (request.getText() == null ? 0 : request.getText().length());
        }
        Map<String, Object> params = buildParams(request);
        log.info("调用百度翻译API，POST请求，URL: {}", endpoint);
        log.info("请求参数：{}", params);
        try {
            // 发送POST请求，参数以表单形式提交
            HttpResponse response = HttpRequest.post(endpoint)
                    .form(params)
                    .execute();
            if (!response.isOk()) {
                return "Error: HTTP " + response.getStatus();
            }
            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.containsKey("error_code")) {
                return "Error: " + json.getStr("error_msg");
            }
            JSONObject result = json.getJSONArray("trans_result").getJSONObject(0);
            return result.getStr("dst");
        } catch (Exception e) {
            return "Translation failed: " + e.getMessage();
        }
    }
}