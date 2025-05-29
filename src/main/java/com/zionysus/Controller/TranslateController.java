package com.zionysus.Controller;

import com.zionysus.DTO.BaiduTranslateRequest;
import com.zionysus.Service.TranslateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TranslateController {

    // 存储不同翻译服务的实现，key为服务名称+"TranslateService"
    private final Map<String, TranslateService> translateServices;

    public TranslateController(Map<String, TranslateService> translateServices) {
        this.translateServices = translateServices;
    }

    @GetMapping("/translate")
    public String translate(@RequestParam String text,
                            @RequestParam String from,
                            @RequestParam String to,
                            @RequestParam(defaultValue = "baidu") String api) {
        TranslateService service = translateServices.get(api + "TranslateService");
        if (service == null) {
            return "错误：不支持的翻译API：" + api;
        }
        BaiduTranslateRequest request=new BaiduTranslateRequest(text,from,to);
        return service.translate(request);
    }
}