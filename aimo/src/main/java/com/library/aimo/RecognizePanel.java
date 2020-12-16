package com.library.aimo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;

import com.aimall.sdk.faceactiondetector.bean.FaceActionType;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.api.FaceActionLiveDelegate;
import com.library.aimo.api.FaceRecognizedDelegate;
import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.CameraCallBack;
import com.library.aimo.util.ImoLog;
import com.library.aimo.widget.CameraContainer;
import com.library.aimo.widget.ClipRelativeLayout;

import java.io.File;
import java.util.Random;


/**
 * 人脸识别，人脸动作 组件
 */

public abstract class RecognizePanel {

    CameraContainer cameraContainer;
    ClipRelativeLayout clipCoverView;

    private Activity context;

    public RecognizePanel(Activity context) {
        this.context = context;
    }

    /**
     * 获取容器，可覆盖该方法设置自定义界面
     *
     * @return
     */
    @SuppressLint("InflateParams")
    protected View getParentView() {
        return LayoutInflater.from(context).inflate(R.layout.inc_face_panel, null);
    }

    protected int getCoverColor() {
        return 0xff0C1529;
    }

    private File cacheDir;

    /**
     * 禁用圆形裁剪
     *
     * @return
     */
    public RecognizePanel disableClip() {
        clipCoverView.setVisibility(View.GONE);
        return this;
    }

    public View onCreate() {
        View parentView = getParentView();
        cameraContainer = parentView.findViewById(R.id.surface);
        clipCoverView = parentView.findViewById(R.id.rl_layout_clip);
        clipCoverView.setBackgroundColor(getCoverColor());
        initCamera(640, 480);

        IMoRecognitionManager.getInstance().init(SettingConfig.getAlgorithmNumThread(), null);

        cacheDir = new File(context.getCacheDir(), "cacheBitmap");
        if (!cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }
        if (isFaceRecognized()) {//人脸识别
            faceRecognizedDelegate = new FaceRecognizedDelegate(new FaceRecognizedDelegate.FaceExtractListener() {
                @Override
                public void onFaceMatch(Bitmap bitmap, float score) {
                    clipCoverView.showSuccess();
                    onFaceRecognized(score, bitmap, getLocalCacheId());
                }

                @Override
                public void onFaceDisappear() {
                    onFaceNotRecognized();
                }

                @Override
                public void onTimeout() {
                    showRecognitionTimeoutDialog();
                }

            });
            faceRecognizedDelegate.setCurrentUserId(getLocalCacheId());
            faceRecognizedDelegate.setCacheDir(cacheDir);
            faceRecognizedDelegate.init();
        } else {//人脸录入
            faceActionLiveDelegate = new FaceActionLiveDelegate(new FaceActionLiveDelegate.FaceActionListener() {

                @Override
                public void onActionRight(int currentAction, int nextAction) {
                    faceActionLiveDelegate.nextAction();
                    onActionChanged(currentAction, actions[nextAction]);
                }

                @Override
                public void onTimeout() {
                    showRecognitionTimeoutDialog();
                }

                @Override
                public void onFaceDisappear() {
                    onFaceNotRecognized();
                }

                @Override
                public void onActionLiveMatch(Bitmap bitmap) {
                    clipCoverView.showSuccess();
                    onFaceRecorded(getLocalCacheId(), bitmap);
                }
            });
            faceActionLiveDelegate.setCacheDir(cacheDir);
            faceActionLiveDelegate.init();
        }
        return parentView;
    }


    public File getCacheDir() {
        return cacheDir;
    }

    private FaceActionLiveDelegate faceActionLiveDelegate;
    private FaceRecognizedDelegate faceRecognizedDelegate;


    RectF areaRect = null;

    private void initCamera(int width, int height) {
        ImoLog.e("initCamera>> " + width + "," + height);
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
                if (null != faceActionLiveDelegate && faceActionLiveDelegate.isEnable()) {
                    RectF currentRect = faceActionLiveDelegate.onPreView(cameraEngine, data);
                    if (areaRect == null) {
                        areaRect = clipCoverView.getArea();
                    }
                    if (currentRect != null && areaRect != null) {
                        onFaceRectStatus(areaRect.contains(currentRect));
                    } else {
                        onFaceRectStatus(false);
                    }
                }

                if (null != faceRecognizedDelegate) {
                    faceRecognizedDelegate.onPreView(cameraEngine, data);
                }
                cameraContainer.requestRender();
            }
        });

        CameraContainer.UiConfig uiConfig = new CameraContainer.UiConfig()
                .showChangeImageQuality(false)
                .showLog(false)
                .showTakePic(false)
                .showDrawPointsView(true)
                .refreshCanvasWhenPointRefresh(true)
                .setCameraRotateAdjust(SettingConfig.getCameraRotateAdjust()) // 特殊设备手动适配
                .setFlipX(SettingConfig.getCameraPreviewFlipX());
        cameraContainer.refreshConfig(uiConfig);

    }

    int[] actions;

    public void startFaceCheck() {
        if (null != faceRecognizedDelegate) {
            faceRecognizedDelegate.startFaceExtract();
        }
        onResume();
    }

    public void startRandomAction(boolean recheck) {
        if (null != faceActionLiveDelegate) {
            actions = new int[2];
            if (new Random().nextBoolean()) {
                actions[0] = FaceActionType.FaceActionTypeMouthOpen;
                int[] allActions = {FaceActionType.FaceActionTypeHeadTurnLeft,
                        FaceActionType.FaceActionTypeHeadTurnRight, FaceActionType.FaceActionTypeNod, FaceActionType.FaceActionTypeBlink};
                actions[1] = allActions[new Random().nextInt(allActions.length)];
            } else {
                actions[0] = FaceActionType.FaceActionTypeBlink;
                int[] allActions = {FaceActionType.FaceActionTypeHeadTurnLeft,
                        FaceActionType.FaceActionTypeHeadTurnRight, FaceActionType.FaceActionTypeNod, FaceActionType.FaceActionTypeMouthOpen};
                actions[1] = allActions[new Random().nextInt(allActions.length)];
            }

            ImoLog.e("actions[1]>> " + actions[1]);
            if (recheck) {
                faceActionLiveDelegate.restart(actions);
            } else {
                faceActionLiveDelegate.setAction(actions);
                onResume();
            }
            onActionChanged(-1, actions[0]);
        }
    }

    public int getTime() {
        if (faceActionLiveDelegate == null) {
            return 10;
        }
        return faceActionLiveDelegate.getCost();
    }

    public void onResume() {
        cameraContainer.onResume();
    }

    public void onPause() {
        cameraContainer.onPause();
    }


    public void onDestroy() {
        if (null != faceActionLiveDelegate) {
            faceActionLiveDelegate.onDestroy();
            faceActionLiveDelegate.setFaceActionListener(null);
        }

        if (null != faceRecognizedDelegate) {
            faceRecognizedDelegate.onDestroy();
        }
        if (cameraContainer != null) {
            cameraContainer.onDestroy();
            cameraContainer = null;
        }
        context = null;
    }


    /**
     * 人脸是否在框内
     */
    protected abstract void onFaceRectStatus(boolean isRight);

    /**
     * 人脸丢失
     */
    protected abstract void onFaceNotRecognized();

    /**
     * 动作正确
     *
     * @param currentAction
     * @param nextAction
     */
    protected abstract void onActionChanged(int currentAction, int nextAction);

    /**
     * 是否是人脸识别，false表示是人脸录入
     *
     * @return
     */
    protected abstract boolean isFaceRecognized();

    /**
     * 获取用户唯一码
     *
     * @return
     */
    protected abstract String getLocalCacheId();

    /**
     * 人脸录入结果
     *
     * @param id
     * @param bitmap
     */
    protected abstract void onFaceRecorded(String id, Bitmap bitmap);

    /**
     * 人脸识别结果
     *
     * @param bitmap
     * @param id
     */
    protected abstract void onFaceRecognized(float score, Bitmap bitmap, String id);

    /**
     * 显示超时弹窗
     */
    protected abstract void showRecognitionTimeoutDialog();

}
