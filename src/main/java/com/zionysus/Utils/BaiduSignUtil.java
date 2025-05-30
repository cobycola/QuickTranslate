package com.zionysus.Utils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class BaiduSignUtil {

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
}

