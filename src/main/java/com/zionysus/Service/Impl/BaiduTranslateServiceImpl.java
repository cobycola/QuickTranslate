package com.zionysus.Service.Impl;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zionysus.DTO.TranslateRequest;
import com.zionysus.DTO.TranslateResponse;
import com.zionysus.Service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public TranslateResponse translate(TranslateRequest request) {
        String salt = java.lang.String.valueOf(System.currentTimeMillis());
        String sign = md5(appid + request.getText() + salt + key);
        Map<String, Object> params = new HashMap<>();
        params.put("q", request.getText());
        params.put("from", request.getFrom());
        params.put("to", request.getTo());
        params.put("appid", appid);
        params.put("salt", salt);
        params.put("sign", sign);

        TranslateResponse response = new TranslateResponse();
        if (request.getText() == null || request.getText().length() > 1000) {
            response.setError_code("400");
            response.setError_msg("文本长度超过百度API单次1000字符限制，请缩短文本。当前长度："
                    + (request.getText() == null ? 0 : request.getText().length()));
            return response;
        }
        log.info("调用百度翻译API，POST请求，URL: {}", endpoint);
        log.info("请求参数：{}", params);
        try {
            HttpResponse httpResponse = HttpRequest.post(endpoint)
                    .form(params)
                    .execute();

            if (!httpResponse.isOk()) {
                response.setError_code(String.valueOf(httpResponse.getStatus()));
                response.setError_msg("HTTP请求失败");
                return response;
            }

            JSONObject json = JSONUtil.parseObj(httpResponse.body());
            if (json.containsKey("error_code")) {
                response.setError_code(json.getStr("error_code"));
                response.setError_msg(json.getStr("error_msg"));
                return response;
            }

            // 解析翻译结果
            response.setFrom(json.getStr("from"));
            response.setTo(json.getStr("to"));
            List<TranslateResponse.Translation> transResults = new ArrayList<>();
            json.getJSONArray("trans_result").forEach(obj -> {
                JSONObject item = (JSONObject) obj;
                TranslateResponse.Translation translation = new TranslateResponse.Translation();
                translation.setSrc(item.getStr("src"));
                translation.setDst(item.getStr("dst"));
                transResults.add(translation);
            });
            response.setTrans_result(transResults);

            return response;

        } catch (Exception e) {
            response.setError_code("500");
            response.setError_msg("Translation failed: " + e.getMessage());
            return response;
        }
    }
}