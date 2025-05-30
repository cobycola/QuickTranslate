package com.zionysus.Service.Impl;

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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("fastApiTranslateService")
public class FastApiTranslateServiceImpl implements TranslateService {

    @Value("${fastapi.endpoint}")
    private String endpoint;

    @Override
    public String translate(TranslateRequest request) {
        try {
            String encodedText = URLEncoder.encode(request.getText(), StandardCharsets.UTF_8.name());
            encodedText = encodedText.replace("+", "%20");
            Map<String, Object> params = new HashMap<>();
            params.put("text", encodedText);
            String requestUrl = endpoint + "?" + params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((p1, p2) -> p1 + "&" + p2)
                    .orElse("");
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
}