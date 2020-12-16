package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.RectF;

import com.library.aimo.EasyLibUtils;
import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.api.IMoSDKManager;
import com.library.aimo.api.StaticOpenApi;
import com.library.aimo.util.ImoLog;
import com.library.aimo.video.record.VideoBuilder;

import java.lang.reflect.Method;

/**
 * 连接imo module的桥梁
 * <p>
 * 可由此卸载imo， 不使用人脸识别的时候，直接注释对imo的调用， 把上方的注释代码解开即可
 */
public class IMoBridge {


    /**
     * IMO SDK初始化回调
     */
    public interface IImoInitListener {
        void onSuccess();

        void onFail(int code);
    }

    /**
     * 生成视频回调
     */
    public interface IImoVideoBuildListener {
        void onSuccess(String path);
    }


    public static abstract class RecognizePanel extends com.library.aimo.RecognizePanel {
        public RecognizePanel(Activity context) {
            super(context);
        }
    }

    /**
     * 初始化EasyLib上下文
     *
     * @param application
     */
    public static void initEasyLibUtils(Application application) {
        EasyLibUtils.init(application);
    }

    /**
     * 本地是否有存储头像
     *
     * @param id
     * @return
     */
    public static boolean existLocalFace(String id) {
        return StaticOpenApi.existLocalFace(id);
    }

    /**
     * 保存人脸特征到本地
     *
     * @param uid
     * @param features
     */
    public static void saveLocalFace(String uid, float[] features) {
        StaticOpenApi.saveLocalFace(uid, features);
    }

    /**
     * 获取cpu类型
     *
     * @return
     */
    public static String getCPUAbi() {
        String arch = "";//cpu类型
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getDeclaredMethod("get", new Class[]{String.class});
            arch = (String) get.invoke(clazz, new Object[]{"ro.product.cpu.abi"});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arch;
    }

    /**
     * 初始化IMO环境
     *
     * @param context
     */
    public static void init(Application context, String key, final IImoInitListener listener) {
        initEasyLibUtils(context);

        IMoSDKManager.KEY = key;

        ImoLog.e("init: key=" + key);
        if (getCPUAbi().equals("x86")) {
            if (listener != null) {
                listener.onFail(-1);
            }
            return;
        }
        IMoSDKManager.get().initImoSDK(new IMoSDKManager.FaceSDKInitListener() {
            @Override
            public void onInitResult(boolean success, int errorCode) {
                if (listener != null) {
                    if (!success) {
                        listener.onFail(errorCode);
                    } else {
                        listener.onSuccess();
                    }
                }
            }
        });
    }

    /**
     * 生成本地视频
     *
     * @param recognizePanel
     * @param listener
     */
    public static void buildVideo(IMoBridge.RecognizePanel recognizePanel, IImoVideoBuildListener listener) {
        //生成视频
        new VideoBuilder(recognizePanel.getCacheDir(), recognizePanel.getTime()) {
            @Override
            public void finish() {
                super.finish();
                listener.onSuccess(VideoBuilder.getOutputVideo().toString());
            }
        }.start();
    }


    /**
     * 释放IMO资源
     */
    public static void release() {
        IMoRecognitionManager.getInstance().release();
        IMoSDKManager.get().destroy();
    }


    /**
     * 比较图片的人脸信息
     *
     * @param feature
     * @param bitmap
     * @return 相似度
     */
    public static int findMatchResults(float[] feature, Bitmap bitmap) {
        return StaticOpenApi.findMatchResults(feature, bitmap);
    }

    /**
     * 获取图片的人脸信息
     *
     * @param bitmap
     * @return
     */
    public static float[] getBitmapFeature(Bitmap bitmap) {
        return StaticOpenApi.getBitmapFeature(bitmap);
    }

    /**
     * 获取图片的头像位置，用于裁剪证件照的头像
     *
     * @param bitmap
     * @return
     */
    public static RectF getBitmapRect(Bitmap bitmap) {
        return StaticOpenApi.getBitmapRect(bitmap);
    }

}
