package com.zionysus.Service.Impl;

import com.zionysus.DTO.BaiduTranslateResponse;
import com.zionysus.DTO.BaiduTranslateRequest;
import com.zionysus.Service.TranslateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service("baiduTranslateService")
public class BaiduTranslateServiceImpl implements TranslateService {

    @Value("${baidu.appid}")
    private String appid;

    @Value("${baidu.key}")
    private String key;

    @Value("${baidu.endpoint:https://fanyi-api.baidu.com/api/trans/vip/translate}")
    private String endpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String urlEncode(String text) {
        try {
            return java.net.URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String translate(BaiduTranslateRequest request) {
        String salt = String.valueOf(System.currentTimeMillis()); // 避免重复
        String text = request.getText();
        String from = request.getFrom();
        String to = request.getTo();
        String sign = md5(appid + text + salt + key);
        String encodedText = urlEncode(text);
        //组合URL 这里注意！！sign不传入urlEncode(text) 否则会54001报错
        String url = String.format("%s?q=%s&from=%s&to=%s&appid=%s&salt=%s&sign=%s",
                endpoint, encodedText, from, to, appid, salt, sign);
        BaiduTranslateResponse response=null;
        try {
            URI uri = URI.create(url);
            response = restTemplate.getForObject(uri, BaiduTranslateResponse.class);
        } catch (Exception e) {
            return "翻译失败：" + e.getMessage();
        } finally {
            System.out.println("响应: " + response);
        }

        if (response == null) {
            return "翻译失败：无响应";
        }

        if (response.getError_code() != null) {
            return "翻译错误：" + response.getError_msg();
        }

        List<BaiduTranslateResponse.Translation> results = response.getTrans_result();
        if (results == null || results.isEmpty()) {
            return "无翻译结果";
        }

        return results.get(0).getDst();
    }


}