package com.library.aimo.config;


import com.library.aimo.util.MathUtil;

/**
 * ================================================
 * 作    者：aguai（吴红斌）Github地址：https://github.com/aguai1
 * 版    本：1.0
 * 创建日期：17-11-17
 * 描    述：通用设置管理类
 * ================================================
 */
public class SettingConfig {

    private static final String SETTING_CAMERAROTATE_ADJUST = "setting_camerarotate_adjust";
    private static final String SETTING_CAMERAPREVIEW_FLIPX = "setting_camerapreview_flipx";

    public static final int DEFAULT_CAMERAROTATE_ADJUST = 0;
    public static final boolean DEFAULT_CAMERAPREVIEW_FLIPX = false;

    @Deprecated
    private static final String SETTING_EYE_CLOSE_THRESHOLD = "setting_eye_close_threshold";
    @Deprecated
    public static final float DEFAULT_EYE_CLOSE_THRESHOLD = 0.5f;

    public static int getCameraRotateAdjust() {
        return (int) SharedPreferencesUtils.get(SETTING_CAMERAROTATE_ADJUST, DEFAULT_CAMERAROTATE_ADJUST);
    }

    public static void setCameraRotateAdjust(int cameraRotateAdjust) {
        SharedPreferencesUtils.put(SETTING_CAMERAROTATE_ADJUST, MathUtil.normalizationRotate(cameraRotateAdjust));
    }

    public static boolean getCameraPreviewFlipX() {
        return (boolean) SharedPreferencesUtils.get(SETTING_CAMERAPREVIEW_FLIPX, DEFAULT_CAMERAPREVIEW_FLIPX);
    }

    public static void setCameraPreviewFlipX(boolean cameraPreviewFlipX) {
        SharedPreferencesUtils.put(SETTING_CAMERAPREVIEW_FLIPX, cameraPreviewFlipX);
    }

    @Deprecated
    public static float getEyeCloseThreshold() {
        return (float) SharedPreferencesUtils.get(SETTING_EYE_CLOSE_THRESHOLD, DEFAULT_EYE_CLOSE_THRESHOLD);
    }

    public static int normalizationRotate(int rotate) {
        while (rotate < 0) {
            rotate += 360;
        }
        return (rotate % 360);
    }

    public static int getAlgorithmNumThread() {
        return 4;
    }
}
