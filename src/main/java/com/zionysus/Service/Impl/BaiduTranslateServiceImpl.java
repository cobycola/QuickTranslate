package com.zionysus.Service.Impl;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zionysus.DTO.TranslateRequest;
import com.zionysus.DTO.TranslateResponse;
import com.zionysus.Exception.BaiduException;
import com.zionysus.Service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

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

    private List<String> splitTextIntoParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return paragraphs;

        // 按连续换行符切分段落
        String[] rawParagraphs = text.split("\\n+");
        for (String para : rawParagraphs) {
            para = para.trim();
            if (!para.isEmpty()) {
                paragraphs.add(para);
            }
        }
        return paragraphs;
    }

    private List<String> splitParagraph(String paragraph, int maxLen) {
        List<String> results = new ArrayList<>();
        //判空
        if(StrUtil.isBlank(paragraph)) {
            return results;
        }
        // 按标点符号切分句子
        String regex = "(?<=[。！？\\.!?])\\s*";
        List<String> sentences = Arrays.asList(paragraph.split(regex));
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() <= maxLen) {
                current.append(sentence);
            } else {
                results.add(current.toString());
                current = new StringBuilder(sentence);
            }
        }
        if(current.length() > 0) {
            results.add(current.toString());
        }
        return results;
    }
    @Override
    public TranslateResponse translate(TranslateRequest request) {
        String originalText = request.getText();
        if (StrUtil.isBlank(originalText)) {
            // 空文本直接返回
            TranslateResponse emptyResp = new TranslateResponse();
            emptyResp.setTrans_result(Collections.emptyList());
            return emptyResp;
        }
        String from = request.getFrom();
        String to = request.getTo();
        // 段落拆分
        List<String> paragraphs = splitTextIntoParagraphs(originalText);
        List<TranslateResponse.Translation> allTrans = new ArrayList<>();
        try{
            // 记录当前翻译次数
            int count=0;
            for (String paragraph : paragraphs) {
                List<String> smallParts;
                // 大于1000字，先拆分成小段
                if (paragraph.length() > 1000) {
                    smallParts = splitParagraph(paragraph, 1000);
                } else {
                    smallParts = Collections.singletonList(paragraph);
                }
                // 逐个翻译
                StringBuilder combinedSrc = new StringBuilder();
                StringBuilder combinedDst = new StringBuilder();
                for (String part : smallParts) {
                    //防止百度翻译API请求过快
                    if(count>0){
                        System.out.println("当前翻译次数："+count);
                        Thread.sleep(1200);
                    }
                    TranslateResponse.Translation partTrans = doTranslate(part,from,to);
                    combinedSrc.append(partTrans.getSrc());
                    combinedDst.append(partTrans.getDst());
                    count++;
                }
                TranslateResponse.Translation paraTrans = new TranslateResponse.Translation();
                paraTrans.setSrc(combinedSrc.toString());
                paraTrans.setDst(combinedDst.toString());
                allTrans.add(paraTrans);
            }
        }catch(BaiduException e){
            log.error("{}：{}",e.getErrorCode(),e.getMessage());
            TranslateResponse errorResp = new TranslateResponse();
            errorResp.setError_code(e.getErrorCode());
            errorResp.setError_msg(e.getMessage());
            return errorResp;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 构建最终响应
        TranslateResponse finalResp = new TranslateResponse();
        finalResp.setFrom(from);
        finalResp.setTo(to);
        finalResp.setTrans_result(allTrans);
        return finalResp;
    }

    public TranslateResponse.Translation doTranslate(String text,String from,String to) {
        String salt = java.lang.String.valueOf(System.currentTimeMillis());
        String sign = md5(appid + text + salt + key);
        Map<String, Object> params = new HashMap<>();
        params.put("q", text);
        params.put("from", from);
        params.put("to", to);
        params.put("appid", appid);
        params.put("salt", salt);
        params.put("sign", sign);
        TranslateResponse response = new TranslateResponse();
        //检查字数是否超过限制
        if (text == null || text.length() > 1000) {
            throw new BaiduException("400", "Error: text length exceeds 1000 characters");
        }
        log.info("调用百度翻译API，POST请求，URL: {}", endpoint);
        log.info("请求参数：{}", params);
        try {
            HttpResponse httpResponse = HttpRequest.post(endpoint)
                    .form(params)
                    .execute();
            // 检查是否翻译成功
            if (!httpResponse.isOk()) {
                throw new BaiduException("400", "Error: HTTP request failed");
            }
            JSONObject json = JSONUtil.parseObj(httpResponse.body());
            if (json.containsKey("error_code")) {
                throw new BaiduException(json.getStr("error_code"), json.getStr("error_msg"));
            }

            // 解析翻译结果
            TranslateResponse.Translation transResult=new TranslateResponse.Translation();
            JSONArray jsonTransResult = json.getJSONArray("trans_result");
            transResult.setSrc(jsonTransResult.getJSONObject(0).getStr("src"));
            transResult.setDst(jsonTransResult.getJSONObject(0).getStr("dst"));
            return transResult;

        } catch (Exception e) {
            throw e;
        }
    }
}