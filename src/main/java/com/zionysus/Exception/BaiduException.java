package com.zionysus.Exception;

public class BaiduException extends RuntimeException{
    private final String errorCode;

    public BaiduException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
