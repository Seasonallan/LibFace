package com.library.aimo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aimall.sdk.faceactiondetector.bean.FaceActionType;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.api.FaceActionLiveDelegate;
import com.library.aimo.api.FaceRecognizedDelegate;
import com.library.aimo.api.IMoRecognitionManager;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.CameraCallBack;
import com.library.aimo.util.ImoLog;
import com.library.aimo.video.record.VideoBuilder;
import com.library.aimo.widget.CameraContainer;
import com.library.aimo.widget.ClipRelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * 人脸识别，人脸动作 组件
 */
public abstract class RecognizePanel {

    CameraContainer cameraContainer;
    ClipRelativeLayout clipCoverView;
    TextView actionView;

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

    /**
     * 构建cameraView
     *
     * @return
     */
    public View onCreate() {
        View parentView = getParentView();
        actionView = parentView.findViewById(R.id.tv_action);
        cameraContainer = parentView.findViewById(R.id.surface);
        clipCoverView = parentView.findViewById(R.id.rl_layout_clip);
        clipCoverView.setBackgroundColor(getCoverColor());
        initCamera(640, 480);

        IMoRecognitionManager.getInstance().init(SettingConfig.getAlgorithmNumThread(), null);

        cacheDir = new File(context.getCacheDir(), "cacheBitmap");
        if (!cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }
        actionView.setVisibility(View.GONE);
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
            faceRecognizedDelegate.setUniqueID(getLocalCacheId());
            faceRecognizedDelegate.setCacheDir(cacheDir);
            faceRecognizedDelegate.init();
        } else {//人脸录入
            faceActionLiveDelegate = new FaceActionLiveDelegate(new FaceActionLiveDelegate.FaceActionListener() {

                @Override
                public void onActionRight(int currentAction, int nextAction) {
                    faceActionLiveDelegate.nextAction();
                    clipCoverView.setProgress(nextAction * 100 / actions.length, true);
                    actionChange(currentAction, actions[nextAction]);
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
                    clipCoverView.setProgress(100, true);
                    onFaceRecorded(getLocalCacheId(), bitmap);
                }
            });
            faceActionLiveDelegate.setCacheDir(cacheDir);
            faceActionLiveDelegate.init();
        }
        return parentView;
    }

    /**
     * 获取文件缓存位置
     *
     * @return
     */
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

    /**
     * 开始人脸识别
     */
    public void startFaceCheck() {
        if (null != faceRecognizedDelegate) {
            faceRecognizedDelegate.startFaceExtract();
        }
        onResume();
        VideoBuilder.clearCaches(getCacheDir());
    }

    /**
     * 开始两个随机动作
     *
     * @param recheck 重新开始
     */
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
            actionChange(-1, actions[0]);
        }
        VideoBuilder.clearCaches(getCacheDir());
    }

    /**
     * 录入人脸
     *
     * @param random 打乱顺序
     */
    public void startRecordByAction(boolean random) {
        actionView.setVisibility(View.VISIBLE);
        clipCoverView.setModeProgress();
        clipCoverView.setProgress(0, true);
        if (null != faceActionLiveDelegate) {
            List<Integer> actionArrays = new ArrayList<>();
            actionArrays.add(FaceActionType.FaceActionTypeHeadTurnLeft);
            actionArrays.add(FaceActionType.FaceActionTypeHeadTurnRight);
            actionArrays.add(FaceActionType.FaceActionTypeNod);
            actionArrays.add(FaceActionType.FaceActionTypeMouthOpen);
            if (random) {
                Collections.shuffle(actionArrays);
            }
            actions = new int[actionArrays.size()];
            for (int i = 0; i < actionArrays.size(); i++) {
                actions[i] = actionArrays.get(i);
            }

            faceActionLiveDelegate.setAction(actions);

            onResume();
            actionChange(-1, actions[0]);
        }
        VideoBuilder.clearCaches(getCacheDir());
    }

    /**
     * 动作正确
     *
     * @param currentAction
     * @param nextAction
     */
    protected void actionChange(int currentAction, int nextAction){
        switch (nextAction) {
            case 1:
                actionView.setText(actionDescriptions[0]);
                break;
            case 2:
                actionView.setText(actionDescriptions[1]);
                break;
            case 4:
                actionView.setText(actionDescriptions[2]);
                break;
            case 8:
                actionView.setText(actionDescriptions[3]);
                break;
            case 32:
                actionView.setText(actionDescriptions[4]);
                break;
        }
        onActionChanged(currentAction, nextAction);
    }

    String[] actionDescriptions = {"向左转头", "向右转头", "上下点头", "眨眨眼", "张张嘴"};

    /**
     * 顺序需要严格按照{"向左转头", "向右转头", "上下点头", "眨眨眼", "张张嘴", };来执行
     * @param strings
     */
    public void setActionDescriptions(String... strings){
        actionDescriptions = strings;
    }

    /**
     * 获取动作执行的耗时
     *
     * @return
     */
    public int getTime() {
        if (faceActionLiveDelegate == null) {
            return 10;
        }
        return faceActionLiveDelegate.getCost();
    }

    /**
     * 相机打开
     */
    public void onResume() {
        cameraContainer.onResume();
    }


    /**
     * pause,相机关闭
     */
    public void onPause() {
        cameraContainer.onPause();
    }

    /**
     * 资源回收
     */
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
