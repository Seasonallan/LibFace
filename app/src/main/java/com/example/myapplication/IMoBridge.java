package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.RectF;

import com.library.aimo.EasyLibUtils;
import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.api.IMoSDKManager;
import com.library.aimo.api.StaticOpenApi;
import com.library.aimo.config.SettingConfig;

import java.lang.reflect.Method;

/**
 * 连接imo module的桥梁
 * 可由此卸载imo
 */
public class IMoBridge {
//
//    public static boolean existLocalFace(String id) {
//        return true;
//    }
//
//    public static void init(Context context, String key, IImoInitListener iImoInitListener) {
//    }
//
//    public static void initFaceRecognitionManager(Runnable runnable) {
//    }
//
//    public static int findMatchResults(float[] feature, Bitmap bitmap) {
//        return -1;
//    }
//
//    public static float[] getBitmapFeature(Bitmap bitmap) {
//        return new float[10];
//    }
//
//    public static RectF getBitmapRect(Bitmap bitmap) {
//        return new RectF();
//    }
//
//    public static abstract class RecognizePanel {
//        public View parentView;
//
//        public RecognizePanel(Activity context) {
//        }
//
//        protected abstract boolean isFaceRecognized();
//
//        protected abstract String getLocalCacheId();
//
//        protected abstract void onFaceRecorded(String id, Bitmap bitmap);
//
//        protected abstract void onFaceRecognized(Bitmap bitmap, String token);
//
//        protected abstract void showRecognitionTimeoutDialog();
//
//        protected void retry() {
//        }
//        public void startRandomAction(boolean recheck) {
//
//        }
//        public File getCacheDir() {
//            return null;
//        }
//        protected abstract void onFaceRectStatus(boolean isRight);
//        protected abstract void onFaceNotRecognized();
//        protected abstract void onActionChanged(int currentAction, int nextAction);
//        public int getTime() {
//            return 10;
//        }
//        public void onCreate() {
//        }
//
//        public void onResume() {
//        }
//
//        public void onPause() {
//        }
//
//        public void onDestroy() {
//        }
//    }



    /**
     * IMO SDK初始化回调
     */
    public interface IImoInitListener {
        void onSuccess();

        void onFail(int code);
    }


    public static abstract class RecognizePanel extends com.library.aimo.RecognizePanel {
        public RecognizePanel(Activity context) {
            super(context);
        }
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
        EasyLibUtils.init(context);
        IMoSDKManager.KEY = key;

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
     * 释放IMO资源
     */
    public static void release() {
        IMoRecognitionManager.getInstance().release();
        IMoSDKManager.get().destroy();
    }

    /**
     * 初始化IMO人脸识别
     *
     * @param runnable
     */
    public static void initFaceRecognitionManager(final Runnable runnable) {
        IMoRecognitionManager.getInstance().init(SettingConfig.getAlgorithmNumThread(), new IMoRecognitionManager.InitListener() {
            @Override
            public void onSucceed() {
                runnable.run();
            }

            @Override
            public void onError(int code, String msg) {
                runnable.run();
            }
        });

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
