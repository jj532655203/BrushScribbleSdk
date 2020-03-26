package com.jj.brush_scribble_sdk.utils;

public class StringUtils {
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().length() <= 0;
    }
}
