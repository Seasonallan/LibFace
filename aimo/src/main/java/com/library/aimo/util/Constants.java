package com.library.aimo.util;

import android.os.Environment;

import com.library.aimo.EasyLibUtils;

import java.io.File;

/**
 * Created by chong on 17-7-11.
 */

public class Constants {
    public static final String DEFAULT_TIME_ZONE = "GMT+08";
    public static final int INVALID_VALUE = 0xFFFFFFFF;
    public static final String CONFIRM_APP_UPDTAE_ACTION_NAME = "android.intent.action.CONFIRM_APP_UPDTAE_ACTION_NAME";

    public static String getRootDir() {
        String dir = null;
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();  //获取根目录
        }
        if (sdDir != null) {
            dir = makeDir(sdDir.toString() + File.separator + "AIMALL" + File.separator);
        }
        return dir;
    }

    public static String getFilePath() {
        String dir = getRootDir();
        if (dir != null) {
            return dir + "social_contact" + File.separator;
        } else {
            String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            absolutePath += File.separator + "AIMALL";
            return absolutePath + File.separator + "social_contact" + File.separator;
        }
    }

    public static String getDeviceDir() {
        return makeDir(getRootDir() + File.separator + "device" + File.separator);
    }

    public static String getSupportPath() {
        return makeDir(getRootDir() + "feedback" + File.separator);
    }


    public static String getBitmapPath() {
        return makeDir(getFilePath() + "bitmap" + File.separator);
    }

    public static String getVideoPath() {
        return makeDir(getFilePath() + "video" + File.separator);
    }


    public static String getCachePath() {
        return makeDir(getFilePath() + "cache" + File.separator);
    }

    public static String getApkPath() {
        return makeDir(getFilePath() + "apk" + File.separator);
    }

    public static String getImageCachePath() {
        return makeDir(getCachePath() + "/imageCache/");
    }


    public static String getProviderPath() {
        return EasyLibUtils.getApp().getPackageName();
    }

    public static String makeDir(String dir) {
        File file = new File(dir);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return dir;
    }
}
