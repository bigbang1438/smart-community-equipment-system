package com.smart.community.common;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * MD5 加密工具（密码存储）
 */
public class Md5Util {

    public static String encrypt(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String raw, String encrypted) {
        return encrypt(raw).equalsIgnoreCase(encrypted);
    }
}
