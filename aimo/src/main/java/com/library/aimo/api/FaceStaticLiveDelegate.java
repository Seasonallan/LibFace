package com.library.aimo.api;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.aimall.core.define.ImoImageFormat;
import com.aimall.core.define.ImoImageOrientation;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.Size;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 静态活体 人脸识别
 */
public class FaceStaticLiveDelegate implements IFaceAction {

    private AtomicBoolean matchSuccess = new AtomicBoolean(false);

    /**
     * 人脸识别回调监听
     */
    public interface IFaceMatchListener {
        void onFaceMatch(Bitmap bitmap, float score, float[] points);
    }

    private IFaceMatchListener IFaceMatchListener;

    public FaceStaticLiveDelegate(IFaceMatchListener IFaceMatchListener) {
        this.IFaceMatchListener = IFaceMatchListener;
    }

    public void startFaceExtract() {
        matchSuccess.set(false);
    }

    public void stopFaceExtract() {
        matchSuccess.set(true);
    }


    @Override
    public void init() {
        // 订阅异步特帧提取回调
        IMoRecognitionManager.getInstance().setAsyncFrameCallback((rectBitmap, data, width, height, format, orientation, flipx, cameraRotate, faceRecognitionInfos, score) -> {
            if (faceRecognitionInfos != null) {
                if (null != faceRecognitionInfos.getPoints()) {
                    if (null != IFaceMatchListener) {
                        IFaceMatchListener.onFaceMatch(rectBitmap, score, faceRecognitionInfos.getPoints());
                    }
                }
            }
        });
    }

    @Override
    public RectF onPreView(BaseCameraEngine cameraEngine, byte[] bytes) {
        if (!IMoSDKManager.get().getInitResult()) {
            return null;
        }
        execFaceExtractTask(cameraEngine, bytes);
        return null;
    }

    File cacheDir;

    /**
     * 设置视频的图片存储位置
     * @param file
     */
    public void setCacheDir(File file){
        this.cacheDir = file;
    }

    private long lastRecordTime = -1;

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

        //采集图片到本地，用于合成视频
        if (System.currentTimeMillis() - lastRecordTime > 1000 / 4 && cacheDir != null){
            lastRecordTime = System.currentTimeMillis();
            Bitmap bitmap = IMoRecognitionManager.getInstance().bytes2bitmap(nv21Data, previewSize.width, previewSize.height, format, orientation, flipx);
            BitmapUtils.saveBitmapCache(cacheDir, bitmap, System.currentTimeMillis()+"");
        }

        //进行人脸识别
        if (!matchSuccess.get()) {
            // 此处输入的相机数据的人脸特帧提取结果会在setAsyncExtractCallback设置的callback中回调出来
            IMoRecognitionManager.getInstance().execFrameBytes(nv21Data, previewSize.width, previewSize.height, format, orientation, flipx, cameraRotate, cacheDir);
        }
    }

    public void onDestroy() {
        // 取消回调
        IMoRecognitionManager.getInstance().setAsyncFrameCallback(null);
        IMoRecognitionManager.getInstance().release();
    }

}
