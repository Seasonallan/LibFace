package com.library.aimo.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aimall.core.ImoSDK;
import com.library.aimo.EasyLibUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 人脸SO库 初始化
 */
public class IMoSDKManager {
    private AtomicBoolean sdkInitSuccess = new AtomicBoolean();
    private Handler handler = new Handler(Looper.getMainLooper());

    public static String KEY;
    public FaceSDKInitListener faceSDKInitListener;
    public void initImoSDK(FaceSDKInitListener listener) {
        this.faceSDKInitListener = listener;
        if(sdkInitSuccess.get()) {
            if (null != faceSDKInitListener) {
                faceSDKInitListener.onInitResult(true, 0);
            }
            return;
        }
        String key = KEY;
        ImoSDK.init(EasyLibUtils.getApp(), key, null, mImoSDKInitListener);
    }

    public ImoSDK.OnInitListener mImoSDKInitListener= new ImoSDK.OnInitListener(){
        @Override
        public void onInitSuccess(String activeMac, long expirationTime) {
            sdkInitSuccess.set(true);
            Log.d("toast>>>", "activeMac=" + activeMac + "," + ",expirationTime=" + expirationTime);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (null != faceSDKInitListener) {
                        faceSDKInitListener.onInitResult(true, 0);
                    }
                }
            });
        }

        @Override
        public void onInitError(final int errorCode, final String message) {
            sdkInitSuccess.set(false);
            Log.d("toast>>>", "onInitError=errorCode" + errorCode + "," + ",message=" + message);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (null != faceSDKInitListener) {
                        faceSDKInitListener.onInitResult(false, errorCode);
                    }
                }
            });
        }
    };

    public interface FaceSDKInitListener {
        void onInitResult(boolean success, int errorCode);
    }

    public void destroy() {
        faceSDKInitListener = null;
        mImoSDKInitListener = null;
        if(sdkInitSuccess.get()) {
            ImoSDK.destroy();
            sdkInitSuccess.set(false);
        }
    }

    public boolean getInitResult() {
        return sdkInitSuccess.get();
    }

    /////////////////////////////////
    private IMoSDKManager() {
    }

    public static IMoSDKManager get() {
        return Inner.INSTANCE;
    }

    private static class Inner {
        public static final IMoSDKManager INSTANCE = new IMoSDKManager();
    }
}
