package com.zionysus.Controller;

import com.zionysus.DTO.TranslateRequest;
import com.zionysus.DTO.TranslateResponse;
import com.zionysus.Service.TranslateService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/translate")
public class TranslateController {

    // 注入多个翻译服务实现，key 形式为 "baiduTranslateService"、"localTranslateService" 等
    private final Map<String, TranslateService> translateServices;

    public TranslateController(Map<String, TranslateService> translateServices) {
        this.translateServices = translateServices;
    }

    @PostMapping
    public TranslateResponse translate (@RequestBody TranslateRequest request) {
        String api=request.getApi();
        TranslateResponse errorResponse = new TranslateResponse();
        if (api == null || api.isEmpty()) {
            errorResponse.setError_code("400");
            errorResponse.setError_msg("错误：未指定翻译API");
            return errorResponse;
        }
        // 获取对应的翻译服务
        TranslateService service = translateServices.get(api + "TranslateService");
        if (service == null) {
            errorResponse.setError_code("400");
            errorResponse.setError_msg("错误：不支持的翻译API：" + api);
            return errorResponse;
        }
        return service.translate(request);
    }
}
