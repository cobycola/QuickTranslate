package com.zionysus.Service.Impl;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zionysus.DTO.TranslateRequest;
import com.zionysus.Service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("fastApiTranslateService")
public class FastApiTranslateServiceImpl implements TranslateService {

    @Value("${fastapi.endpoint}")
    private String endpoint;

    private Map<String, Object> buildParams(TranslateRequest request) throws UnsupportedEncodingException {
        String encodedText = URLEncoder.encode(request.getText(), StandardCharsets.UTF_8.name());
        encodedText = encodedText.replace("+", "%20");
        Map<String, Object> params = new HashMap<>();
        params.put("text", encodedText);
        return params;
    }

    @Override
    public String translateByGet(TranslateRequest request) {
        try {
            Map<String, Object> params = buildParams(request);
            UrlBuilder urlBuilder = UrlBuilder.of(endpoint, Charset.forName("UTF-8"));
            params.forEach((k, v) -> urlBuilder.addQuery(k, String.valueOf(v)));
            String requestUrl = urlBuilder.build();
            log.info("调用FastAPI，URL: {}", requestUrl);
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
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String translateByPost(TranslateRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("text", request.getText());
        log.info("调用FastAPI POST，URL: {}", endpoint);
        try {
            HttpResponse response = HttpRequest.post(endpoint)
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