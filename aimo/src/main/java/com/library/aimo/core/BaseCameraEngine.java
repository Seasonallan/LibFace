package com.library.aimo.core;

import android.Manifest;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.library.aimo.EasyLibUtils;
import com.library.aimo.util.ImoLog;
import com.library.aimo.util.WeakHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public abstract class BaseCameraEngine {
    protected static final String TAG = "CameraEngine";
    protected static Handler handler;
    protected static final HandlerThread handlerThread = new HandlerThread("CameraEngineThread");
    protected static final Set<Integer> openedCameraIds = new HashSet<>();
    private WeakHandler mainHandler;

    static {
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        ImoLog.d(TAG, "camera thread init");
    }

    protected int mCameraId = CameraDefine.CAMERA_ID_FRONT;
    protected Size mPreviewSize = new Size(1920, 1080);
    protected Point mFpsRange;
    protected String mFocusMode = null;
    protected int mExposureCompensation = 0;
    protected SurfaceTexture mSurfaceTexture = null;

    protected Context mContext;
    protected AppCompatActivity mActivity;
    protected int mActivityRotate;
    protected final Vector<CameraCallBack> cameraCallBacks = new Vector<>();

    // FLAGS 超过两个换成按位读取
    // 预览目标是否自定义
    private boolean mIsPreviewTargetCustom;

    public BaseCameraEngine(Context context) {
        this(context, false);
    }


    public BaseCameraEngine(Context context, boolean isPreviewTargetCustom) {
        mContext = context;
        mIsPreviewTargetCustom = isPreviewTargetCustom;
        if (context instanceof AppCompatActivity) {
            this.mActivity = (AppCompatActivity) context;
        } else if (null != EasyLibUtils.sTopActivityWeakRef) {
            this.mActivity = (AppCompatActivity) EasyLibUtils.sTopActivityWeakRef.get();
        }
        mActivityRotate = CameraUtils.getActivityDisplayOrientation(this.mActivity);
        mainHandler = new WeakHandler(Looper.getMainLooper());
        checkAnr();
    }

    public static void runOnCameraThread(Runnable runnable) {
        if (Thread.currentThread() == handlerThread) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    private void checkAnr() {
        final Semaphore semaphore = new Semaphore(0);
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!semaphore.tryAcquire()) {
                    ImoLog.d(TAG, "!相机线程卡死");
                    synchronized (cameraCallBacks) {
                        for (CameraCallBack cameraCallBack : cameraCallBacks) {
                            if (cameraCallBack != null) {
                                cameraCallBack.openCameraError(new Exception("相机线程卡死"));
                            }
                        }
                    }
                }
                checkAnr();
            }
        }, 20 * 1000);

    }

    public void setCameraId(final int cameraId) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "setCameraId 0 cameraId=" + cameraId);
                mCameraId = cameraId;
                ImoLog.d(TAG, "setCameraId 1");
            }
        });
    }

    public void setPreviewSize(final Size previewSize) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "setPreviewSize 0 previewSize=" + previewSize);
                mPreviewSize = previewSize;
                ImoLog.d(TAG, "setPreviewSize 1");
            }
        });
    }

    public void setFocusMode(final String focusMode) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "setFocusMode 0 focusMode=" + focusMode);
                mFocusMode = focusMode;
                ImoLog.d(TAG, "setFocusMode 1");
            }
        });
    }

    public void setExposureCompensation(final int exposureCompensation) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "setExposureCompensation 0 exposureCompensation=" + exposureCompensation);
                mExposureCompensation = exposureCompensation;
                ImoLog.d(TAG, "setExposureCompensation 1");
            }
        });
    }

    public void setSurfaceTexture(final SurfaceTexture surfaceTexture) {
        if (!isPreviewTargetCustom()) {
            throw new RuntimeException("preview target is not custom model!");
        }
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "setSurfaceTexture 0 surfaceTexture=" + surfaceTexture);
                mSurfaceTexture = surfaceTexture;
                ImoLog.d(TAG, "setSurfaceTexture 1");
            }
        });
    }

    FastPermissions fastpermissions;
    public void onResume() {
        ImoLog.d(TAG, "onResume 0 mActivity=" + mActivity);
        if (null == mActivity) {
            openCamera();
        } else {
            if (fastpermissions == null){
                fastpermissions = new FastPermissions(mActivity);
                fastpermissions.need(Manifest.permission.CAMERA)
                        .subscribe(new FastPermissions.Subscribe() {
                            @Override
                            public void onResult(int requestCode, boolean allGranted, String[] permissions) {
                                if (allGranted){
                                    //权限允许后进行的操作
                                    openCamera();
                                }else{
                                    //权限被拒绝
                                    synchronized (cameraCallBacks) {
                                        for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                            if (cameraCallBack != null) {
                                                cameraCallBack.openCameraError(new Exception("获取权限失败"));
                                            }
                                        }
                                    }
                                }
                            }
                        }).request(1);
            }

        }
        ImoLog.d(TAG, "onResume 1");
    }

    public void onPause() {
        ImoLog.d(TAG, "onPause 0");
        closeCamera();
        ImoLog.d(TAG, "onPause 1");
    }

    public void addCameraCallBack(CameraCallBack cameraCallBack) {
        ImoLog.d(TAG, "addCameraCallBack cameraCallBack=" + cameraCallBack);
        synchronized (cameraCallBacks) {
            if (null != cameraCallBack && !cameraCallBacks.contains(cameraCallBack)) {
                cameraCallBacks.add(cameraCallBack);
            }
        }
    }

    public void removeCameraCallBack(CameraCallBack cameraCallBack) {
        ImoLog.d(TAG, "removeCameraCallBack cameraCallBack=" + cameraCallBack);
        synchronized (cameraCallBacks) {
            if (cameraCallBack != null) {
                cameraCallBacks.remove(cameraCallBack);
            }
        }
    }

    public void switchPreviewSize(final Size size) {
        ImoLog.d(TAG, "switchPreviewSize size=" + size.width + "x" + size.height);
        stopPreview();
        setPreviewSize(new Size(size.width, size.height));
        startPreview();
    }

    public void switchCamera() {
        ImoLog.d(TAG, "switchCamera");
        List<Integer> cameraIdList = getSupportCameraId();
        int currentCameraIndex = cameraIdList.indexOf(mCameraId);
        if (currentCameraIndex >= 0) {
            ++currentCameraIndex;
            currentCameraIndex %= cameraIdList.size();
        } else {
            currentCameraIndex = 0;
        }
        switchId(cameraIdList.get(currentCameraIndex));
    }

    public void switchId(int cameraId) {
        closeCamera();
        setCameraId(cameraId);
        openCamera();
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraDefine.CAMERA_ID_FRONT;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public Point getFpsRange() {
        return mFpsRange;
    }

    public int getExposureCompensation() {
        return mExposureCompensation;
    }

    /**
     * 获取相机预览数据逆时针旋转到activity方向所需要的旋转角度
     */
    public int getCameraRotate() {
        return CameraUtils.getImageOrient(getCameraOrientation(), mActivityRotate, mCameraId);
    }

    /**
     * activity旋转角度
     */
    public int getActivityRotate() {
        return mActivityRotate;
    }

    protected boolean isPreviewTargetCustom() {
        return mIsPreviewTargetCustom;
    }

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract List<Integer> getSupportCameraId();

    public abstract List<Size> getSupportPreviewSize();

    protected abstract void openCamera();

    protected abstract void closeCamera();

    protected abstract int getCameraOrientation();

    public abstract int getPreviewFormat();
}
