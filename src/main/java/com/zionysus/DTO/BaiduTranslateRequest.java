package com.zionysus.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaiduTranslateRequest {
    private String text;
    private String from;
    private String to;
}
