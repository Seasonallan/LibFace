package com.example.myapplication;


import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;

public class Bitmap2FileUtils {
    /**
     * 保存图片到缓存文件
     */
    public static String saveBitmapCache(File appDir, Bitmap bitmap, String name) {
        if (bitmap == null) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        if (!TextUtils.isEmpty(name)) {
            fileName = name + ".jpg";
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.toString();
    }
 
}