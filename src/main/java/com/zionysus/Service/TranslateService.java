package com.zionysus.Service;

import com.zionysus.DTO.TranslateRequest;

public interface TranslateService {
    String translateByGet(TranslateRequest request);
    String translateByPost(TranslateRequest request);
}
