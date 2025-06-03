package com.zionysus.Service;

import com.zionysus.DTO.TranslateRequest;
import com.zionysus.DTO.TranslateResponse;

public interface TranslateService {
    TranslateResponse translate(TranslateRequest request);
}
