package com.zionysus.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaiduTranslateResponse {
    private String from;
    private String to;
    private List<Translation> trans_result;
    private String error_code;
    private String error_msg;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Translation {
        private String src;
        private String dst;
    }
}