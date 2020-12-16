package com.library.aimo.api;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.aimall.core.define.ImoImageFormat;
import com.aimall.core.define.ImoImageOrientation;
import com.aimall.sdk.extractor.ImoFaceExtractor;
import com.aimall.sdk.extractor.bean.ImoFaceFeature;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.config.SharedPreferencesUtils;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.Size;
import com.library.aimo.util.ImoLog;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 人脸识别，判断输入跟本地图片对比
 */
public class FaceRecognizedDelegate implements IFaceAction {
    private String currentUserId;
    private long startExtractTime;
    private AtomicBoolean matchSuccess = new AtomicBoolean(false);
    private AtomicBoolean matchTimeout = new AtomicBoolean(false);

    public interface FaceExtractListener {
        void onFaceMatch(Bitmap bitmap);

        void onFaceDisappear();

        void onTimeout();
    }


    private static final long MATCH_TIMEOUT_TIME = 10 * 1000; //匹配超时时间
    private FaceExtractListener faceExtractListener;

    public FaceRecognizedDelegate(FaceExtractListener faceExtractListener) {
        this.faceExtractListener = faceExtractListener;
    }

    float[] features;

    public void startFaceExtract() {
        features = StaticOpenApi.getLocalFace(currentUserId);
        matchSuccess.set(false);
        matchTimeout.set(false);
        startExtractTime = System.currentTimeMillis();
    }


    @Override
    public void init() {
        // 订阅异步特帧提取回调
        IMoRecognitionManager.getInstance().setAsyncFrameCallback(new IMoRecognitionManager.AsyncFrameCallback() {
            @Override
            public void onFaceDisappear() {
                if (null != faceExtractListener) {
                    faceExtractListener.onFaceDisappear();
                }
            }

            @Override
            public void onFaceMatch(byte[] data, int width, int height, ImoImageFormat format, ImoImageOrientation orientation, boolean flip, int cameraRotate, ImoFaceFeature[] imoFaceFeatures, float score) {
                if (null == features) {
                    return;
                }
                ImoLog.e("识别分数："+ score);

                float matchScore = -1;
                if (imoFaceFeatures != null && imoFaceFeatures.length > 0){
                    float compare = ImoFaceExtractor.compare(imoFaceFeatures[0].getFeatures(), features);
                    ImoLog.e("与本地特征值对比分数："+ compare);
                    matchScore = Math.max(matchScore, compare);
                }
                if (matchScore >= 0.9f) {//匹配到人脸，并且对比分值到90分
                    matchSuccess.set(true);
                    if (!matchTimeout.get()) {
                        if (null != faceExtractListener) {
                            faceExtractListener.onFaceMatch(IMoRecognitionManager.getInstance().bytes2bitmap(data, width, height, format, orientation, flip));
                        }
                    }
                }
            }
        });
    }


    public void setCurrentUserId(String usedId) {
        currentUserId = usedId;
    }


    public RectF onPreView(BaseCameraEngine cameraEngine, byte[] nv21Data) {
        if (!IMoSDKManager.get().getInitResult()) {
            return null;
        }

        //在指定时间内检测
        execFaceExtractTask(cameraEngine, nv21Data);
        return null;
    }

    private void execFaceExtractTask(BaseCameraEngine cameraEngine, byte[] nv21Data) {
        // 相机预览的图片分辨率大小
        Size previewSize = cameraEngine.getPreviewSize();
        // 获取到相机图片方向逆时针旋转到屏幕方向需要旋转的角度
        int cameraRotate = cameraEngine.getCameraRotate();
        // 由于android设备碎片化严重，上面获取方向的方法可能不正确，此处提供一个设置项手动更正
        // 如果遇到相机方向不正确的问题可以在主页右上角进入设置中调整
        cameraRotate += SettingConfig.getCameraRotateAdjust();
        cameraRotate = SettingConfig.normalizationRotate(cameraRotate);

        ImoImageFormat format = ImoImageFormat.IMO_IMAGE_NV21;
        ImoImageOrientation orientation = ImoImageOrientation.fromDegreesCCW(cameraRotate);

        // 如果demo相机预览是存在左右镜像的情况那么此处提供设置项调整，同样在设置界面中调整
        boolean flipx = cameraEngine.isFrontCamera();
        if (SettingConfig.getCameraPreviewFlipX()) {
            flipx = !flipx;
        }

        if (!matchSuccess.get() && !matchTimeout.get()) {
            long now = System.currentTimeMillis();
            if ((now - startExtractTime) <= MATCH_TIMEOUT_TIME) { //20秒内检测
                // 此处输入的相机数据的人脸特帧提取结果会在setAsyncExtractCallback设置的callback中回调出来
                IMoRecognitionManager.getInstance().execFrameBytes(nv21Data, previewSize.width, previewSize.height, format, orientation, flipx, cameraRotate, null);
            } else { //超过时间，弹出提示
                matchTimeout.set(true);
                if (!matchSuccess.get()) { //已经匹配成功就不谈超时提示了
                    if (null != faceExtractListener) {
                        faceExtractListener.onTimeout();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        // 取消回调
        IMoRecognitionManager.getInstance().setAsyncFrameCallback(null);
        IMoRecognitionManager.getInstance().release();
    }

}
