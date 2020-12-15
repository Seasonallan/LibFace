package com.library.aimo.util;

public class MathUtil {
    public static int normalizationRotate(int rotate) {
        while (rotate < 0) {
            rotate += 360;
        }
        return (rotate % 360);
    }
}
