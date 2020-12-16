package com.library.aimo.api;

import android.os.Handler;
import android.os.Looper;

import com.aimall.core.ImoSDK;
import com.library.aimo.EasyLibUtils;
import com.library.aimo.util.ImoLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 人脸SO库 初始化
 */
public class IMoSDKManager {
    private AtomicBoolean sdkInitSuccess = new AtomicBoolean();
    private Handler handler = new Handler(Looper.getMainLooper());

    public static String KEY;
    public FaceSDKInitListener faceSDKInitListener;

    private IMoSDKManager() {
    }

    private static volatile IMoSDKManager sIMoSDKManager;

    public static IMoSDKManager get() {
        if (sIMoSDKManager == null) {
            synchronized (IMoSDKManager.class) {
                if (sIMoSDKManager == null) {
                    sIMoSDKManager = new IMoSDKManager();
                }
            }
        }
        return sIMoSDKManager;
    }

    public void initImoSDK(FaceSDKInitListener listener) {
        this.faceSDKInitListener = listener;
        if (sdkInitSuccess.get()) {
            if (null != faceSDKInitListener) {
                faceSDKInitListener.onInitResult(true, 0);
            }
            return;
        }
        String key = KEY;
        ImoLog.e("ImoSDK.init");
        ImoSDK.init(EasyLibUtils.getApp(), key, null, mImoSDKInitListener);
    }

    public ImoSDK.OnInitListener mImoSDKInitListener = new ImoSDK.OnInitListener() {
        @Override
        public void onInitSuccess(String activeMac, long expirationTime) {
            sdkInitSuccess.set(true);
            ImoLog.e("activeMac=" + activeMac + "," + ",expirationTime=" + expirationTime);
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
            ImoLog.e("onInitError=errorCode" + errorCode + "," + ",message=" + message);
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
        if (sdkInitSuccess.get()) {
            ImoLog.e("ImoSDK.destroy");
            ImoSDK.destroy();
            sdkInitSuccess.set(false);
        }
        sIMoSDKManager = null;
    }

    public boolean getInitResult() {
        return sdkInitSuccess.get();
    }


}
