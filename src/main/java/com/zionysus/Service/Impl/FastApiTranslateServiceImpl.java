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

@Slf4j
@Service("fastApiTranslateService")
public class FastApiTranslateServiceImpl implements TranslateService {

    @Value("${fastapi.endpoint}")
    private String endpoint;

    @Override
    public TranslateResponse translate(TranslateRequest request) {
        TranslateResponse response = new TranslateResponse();
        Map<String, Object> params = new HashMap<>();
        params.put("text", request.getText());
        log.info("调用FastAPI POST，URL: {}", endpoint);
        log.info("请求参数：{}",params);
        try {
            HttpResponse httpResponse = HttpRequest.post(endpoint)
                    .body(JSONUtil.toJsonStr(params))
                    .header("Content-Type", "application/json")
                    .execute();
            if (!httpResponse.isOk()) {
                response.setError_code(String.valueOf(httpResponse.getStatus()));
                response.setError_msg("HTTP请求失败");
                return response;
            }
            JSONObject json = JSONUtil.parseObj(httpResponse.body());
            if (json.getStr("error_code")!=null) {
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