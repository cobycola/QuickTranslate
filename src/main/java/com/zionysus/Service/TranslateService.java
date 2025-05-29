package com.zionysus.Service;

import com.zionysus.DTO.BaiduTranslateRequest;

public interface TranslateService {
    String translate(BaiduTranslateRequest request);
}
