package com.library.aimo.api;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.aimall.core.ImoErrorCode;
import com.aimall.core.define.ImoImageFormat;
import com.aimall.core.define.ImoImageOrientation;
import com.aimall.sdk.faceactiondetector.ImoFaceActionLiveness;
import com.aimall.sdk.faceactiondetector.bean.ImoFaceActionInfo;
import com.aimall.sdk.faceactiondetector.utils.ImoFaceActionInfoUtil;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.util.ImoLog;
import com.library.aimo.util.MathUtil;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.Size;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 带动作的人脸识别（交互式活体检测）
 */
public class FaceActionLiveDelegate implements IFaceAction {
    private ImoFaceActionLiveness imoFaceActionDetector;

    private FaceActionListener faceActionListener;

    private boolean firstActionMach = false;//截取一张图片
    private ImoFaceActionLiveness.ActionLiveListener actionLiveListener = new ImoFaceActionLiveness.ActionLiveListener() {
        @Override
        public void onUserDisappear() {
            ImoLog.e("onUserDisappear>>> ");
            if (faceActionListener != null) {
                faceActionListener.onFaceDisappear();
            }
        }

        @Override
        public void onActionDetectorSucceed(int faceActionType, int nextAction) {
            ImoLog.e("onActionLiveMatch>>> " + faceActionType + ", " + nextAction);
            firstActionMach = true;
            if (faceActionListener != null) {
                faceActionListener.onActionRight(faceActionType, faceActionType + 1);
            }
        }

        @Override
        public void onActionCheckFinish(Bitmap bitmap) {
            ImoLog.e("onActionCheckFinish>>> " + bitmap + ", 是否被回收？" + bitmap.isRecycled());
            matchTimeout.set(true);
            if (faceActionListener != null) {
                if (bitmap == null || bitmap.isRecycled()) {
                    faceActionListener.onActionLiveMatch(faceBitmap);
                } else {
                    faceActionListener.onActionLiveMatch(bitmap);
                }
            }
        }

    };

    public void setFaceActionListener(FaceActionListener faceActionListener) {
        this.faceActionListener = faceActionListener;
    }

    public FaceActionLiveDelegate(FaceActionListener faceActionListener) {
        this.faceActionListener = faceActionListener;
    }

    @Override
    public void init() {
        imoFaceActionDetector = new ImoFaceActionLiveness();
        int errorCode = imoFaceActionDetector.init();
        if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
            ImoLog.e("imoFaceActionDetector.init失败！！！errorCode=" + errorCode);
        }
        imoFaceActionDetector.setActionLiveListener(actionLiveListener);
    }

    File cacheDir;

    /**
     * 设置视频的图片存储位置
     *
     * @param file
     */
    public void setCacheDir(File file) {
        this.cacheDir = file;
    }

    private long lastRecordTime = -1;

    private AtomicBoolean matchTimeout = new AtomicBoolean(false);
    private static final long MATCH_TIMEOUT_TIME = 10 * 1000; //匹配超时时间
    private long startExtractTime;

    public boolean isEnable(){
        if (checkFaceActionType == null || matchTimeout.get()) {
            return false;
        }
        if (!IMoSDKManager.get().getInitResult()) {
            return false;
        }
        return true;
    }

    @Override
    public RectF onPreView(BaseCameraEngine cameraEngine, byte[] bytes) {
        if (!isEnable()) {
            return null;
        }

        Size previewSize = cameraEngine.getPreviewSize();
        int cameraRotate = MathUtil.normalizationRotate(cameraEngine.getCameraRotate() + SettingConfig.getCameraRotateAdjust());

        ImoImageFormat format = ImoImageFormat.IMO_IMAGE_NV21;
        ImoImageOrientation orientation = ImoImageOrientation.fromDegreesCCW(cameraRotate);

        // 如果demo相机预览是存在左右镜像的情况那么此处提供设置项调整，同样在设置界面中调整
        boolean flipx = cameraEngine.isFrontCamera();
        if (SettingConfig.getCameraPreviewFlipX()) {
            flipx = !flipx;
        }

        //采集图片到本地，用于合成视频
        if (System.currentTimeMillis() - lastRecordTime > 1000 / 4 && cacheDir != null) {
            lastRecordTime = System.currentTimeMillis();
            Bitmap bitmap = IMoRecognitionManager.getInstance().bytes2bitmap(bytes, previewSize.width, previewSize.height, format, orientation, flipx);
            BitmapUtils.saveBitmapCache(cacheDir, bitmap, System.currentTimeMillis() + "");
            if (firstActionMach) {
                firstActionMach = false;
                faceBitmap = bitmap;
            } else {
                if (bitmap != null && !bitmap.isRecycled())
                    bitmap.recycle();
            }
        }

        if (!matchTimeout.get()) {
            long now = System.currentTimeMillis();
            if ((now - startExtractTime) <= MATCH_TIMEOUT_TIME) { //10秒内检测
                ImoFaceActionInfo imoFaceActionInfo = imoFaceActionDetector.execFrameBytes(bytes, previewSize.width, previewSize.height, format, orientation);
                if (imoFaceActionInfo != null) {
                    imoFaceActionInfo = ImoFaceActionInfoUtil.convertImoFaceActionInfo(imoFaceActionInfo, previewSize.width, previewSize.height, cameraRotate, flipx);
                    //===============================================================================
                    // RectF rect = imoFaceActionInfo.getRect();
                    // 可以通过该rect的大小来判断人脸大小。来做一下引导。让他靠前或者靠后。
                    // 如果imoFaceActionInfo为空则证明画面中没有人脸。
                    //===============================================================================
                    return imoFaceActionInfo.getRect();
                }
            } else { //超过时间，弹出提示
                matchTimeout.set(true);
                if (null != faceActionListener) {
                    faceActionListener.onTimeout();
                }
            }
        }

        return null;
    }

    public Bitmap faceBitmap;

    public int getCost() {
        return (int) (System.currentTimeMillis() - startExtractTime) / 1000;
    }


    @Override
    public void onDestroy() {
        if (faceBitmap != null && !faceBitmap.isRecycled()) {
            faceBitmap.recycle();
            faceBitmap = null;
        }
        imoFaceActionDetector.destroy();
    }

    public void nextAction() {
        imoFaceActionDetector.startCheckNextAction();
    }

    int[] checkFaceActionType;

    public void setAction(int[] type) {
        firstActionMach = false;
        this.checkFaceActionType = type;
        matchTimeout.set(false);
        startExtractTime = System.currentTimeMillis();
        imoFaceActionDetector.configCheckActionList(checkFaceActionType);
    }

    public void restart(int[] type){
        firstActionMach = false;
        this.checkFaceActionType = type;
        imoFaceActionDetector.configCheckActionList(checkFaceActionType);
        imoFaceActionDetector.restartCheck();
    }


    public interface FaceActionListener {
        void onActionLiveMatch(Bitmap bitmap);

        void onActionRight(int currentAction, int nextAction);

        void onTimeout();

        void onFaceDisappear();
    }
}
