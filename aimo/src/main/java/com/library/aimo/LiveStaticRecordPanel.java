package com.library.aimo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;

import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.api.FaceStaticLiveDelegate;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.CameraCallBack;
import com.library.aimo.core.Size;
import com.library.aimo.widget.CameraContainer;

import java.io.File;

public abstract class LiveStaticRecordPanel {

    public CameraContainer cameraContainer;

    FaceStaticLiveDelegate faceActionDelegate;

    private Activity context;

    public LiveStaticRecordPanel(Activity context) {
        this.context = context;
    }

    protected abstract void onFaceMatch(Bitmap bitmap, float score, float[] points);

    /**
     * 获取容器，可覆盖该方法设置自定义界面
     *
     * @return
     */
    protected CameraContainer getCameraView() {
        return new CameraContainer(context);
    }


    public void onCreate() {
        faceActionDelegate = new FaceStaticLiveDelegate(new FaceStaticLiveDelegate.IFaceMatchListener() {
            @Override
            public void onFaceMatch(Bitmap bitmap, float score, float[] points) {
                LiveStaticRecordPanel.this.onFaceMatch(bitmap, score, points);
            }
        });

        cameraContainer = getCameraView();

        faceActionDelegate.init();

        initCamera(640, 480);
        IMoRecognitionManager.getInstance().init(SettingConfig.getAlgorithmNumThread(), new IMoRecognitionManager.InitListener() {
            @Override
            public void onSucceed() {
                faceActionDelegate.startFaceExtract();
            }

            @Override
            public void onError(int code, String msg) {
                faceActionDelegate.startFaceExtract();
            }
        });
    }


    File cacheDir;

    private void initCamera(int width, int height) {
        cacheDir = new File(context.getCacheDir(), "cacheBitmap");
        if (!cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }
        if (faceActionDelegate != null){
            faceActionDelegate.setCacheDir(cacheDir);
        }
        cameraContainer.setPreviewSize(width, height);
        cameraContainer.addCameraCallBack(new CameraCallBack() {
            @Override
            public void openCameraError(Exception e) {
                if (null != e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void openCameraSucceed(BaseCameraEngine cameraEngine, int cameraId) {
                cameraEngine.startPreview();
            }

            @Override
            public void onPreviewFrame(BaseCameraEngine cameraEngine, byte[] data) {
                if (faceActionDelegate != null) {
                    RectF rectF = faceActionDelegate.onPreView(cameraEngine, data);
                    //rectView.setRectF(rectF);
                }
                cameraContainer.requestRender();
            }
        });

        CameraContainer.UiConfig uiConfig = new CameraContainer.UiConfig()
                .showChangeImageQuality(false)
                .showLog(false)
                .showTakePic(false)
                .showSwitchCamera(false)
                .showDrawPointsView(false)
                .refreshCanvasWhenPointRefresh(true)
                .setCameraRotateAdjust(SettingConfig.getCameraRotateAdjust())
                .setFlipX(SettingConfig.getCameraPreviewFlipX());
        cameraContainer.refreshConfig(uiConfig);
    }

    public void onResume() {
        cameraContainer.onResume();
    }

    public void onPause() {
        cameraContainer.onPause();
    }

    public void onDestroy() {
        if (faceActionDelegate != null) {
            faceActionDelegate.stopFaceExtract();
            faceActionDelegate.onDestroy();
        }
        context = null;
    }

    public void startFaceExtract() {
        faceActionDelegate.startFaceExtract();
    }

    public void stopFaceExtract() {
        faceActionDelegate.stopFaceExtract();
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public Size getPreviewSize() {
        return cameraContainer.getPreviewSize();
    }
}
