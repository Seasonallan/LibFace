package com.library.aimo.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.library.aimo.util.ImoLog;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.CameraCallBack;
import com.library.aimo.core.CameraUtils;
import com.library.aimo.core.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraEngine extends BaseCameraEngine {

    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCameraCaptureSession = null;
    private ImageReader mImageReader = null;
    private List<Size> mSupportPreviewSize;
    private int mCameraOrientation = 0;
    private int mPreviewFromat;
    private boolean isPreviewing = false;

    public CameraEngine(Context context) {
        super(context);
    }

    public CameraEngine(Context context, boolean isPreviewTargetCustom) {
        super(context, isPreviewTargetCustom);
    }

    @Override
    public void startPreview() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ImoLog.d(TAG, "startPreview 0 mCamera=" + mCameraDevice + ",isPreviewing=" + isPreviewing);
                    if (null != mCameraDevice && !isPreviewing) {
//                        parameters.setFocusMode(mFocusMode);
//                        parameters.setPreviewFpsRange(mFpsRange.x, mFpsRange.y);
//                        parameters.setExposureCompensation(mExposureCompensation);
                        mPreviewFromat = ImageFormat.NV21;
                        mImageReader = ImageReader.newInstance(mPreviewSize.width, mPreviewSize.height, ImageFormat.YUV_420_888, 2);
                        ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                try (Image image = reader.acquireNextImage()) {
                                    if (image != null) {
                                        int rightLenght = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(image.getFormat()) / Byte.SIZE;
                                        byte[] data = ImageUtil.image2Yuv420888ToMat(image);
                                        if (data.length == rightLenght) {
                                            synchronized (cameraCallBacks) {
                                                for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                                    if (cameraCallBack != null) {
                                                        cameraCallBack.onPreviewFrame(CameraEngine.this, data);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        };
                        mImageReader.setOnImageAvailableListener(readerListener, handler);
                        final CaptureRequest.Builder mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
//                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
//                        Integer integer = mCaptureRequestBuilder.get(CaptureRequest.CONTROL_AWB_MODE);
//                        L.i("whb","CONTROL_AWB_MODE:"+integer);
                        final Semaphore waiter = new Semaphore(0);
                        mCameraDevice.createCaptureSession(Arrays.asList(
                                mImageReader.getSurface()
                        ), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                ImoLog.d(TAG, "startPreview onConfigured");
                                mCameraCaptureSession = session;
                                try {
                                    mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, new Handler(Looper.getMainLooper()));
                                    isPreviewing = true;
                                } catch (CameraAccessException e) {
                                    ImoLog.e(TAG, "startPreview onConfigured setRepeatingRequest failed, e=" + e);
                                    e.printStackTrace();
                                }
                                waiter.release();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                ImoLog.e(TAG, "startPreview onConfigureFailed");
                                waiter.release();
                            }
                        }, new Handler(Looper.getMainLooper()));
                        waiter.acquire();
                    }
                } catch (Exception e) {
                    ImoLog.e(TAG, "startPreview failed, e=" + e);
                    e.printStackTrace();
                }
                ImoLog.d(TAG, "startPreview 1");
            }
        });
    }

    @Override
    public void stopPreview() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "stopPreview 1");
                if (null != mCameraCaptureSession) {
                    try {
                        mCameraCaptureSession.close();
                    } catch (Exception e) {
                        ImoLog.e(TAG, "stopPreview mCameraCaptureSession.close() fail, e=" + e);
                    }
                    mCameraCaptureSession = null;
                }
                if (null != mImageReader) {
                    try {
                        mImageReader.close();
                    } catch (Exception e) {
                        ImoLog.e(TAG, "stopPreview mImageReader.close() fail, e=" + e);
                    }
                    mImageReader = null;
                }
                ImoLog.d(TAG, "stopPreview 1");
            }
        });
    }

    @Override
    public List<Integer> getSupportCameraId() {
        List<Integer> cameraIdList = new ArrayList<>();
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (null != cameraManager) {
                for (String id : cameraManager.getCameraIdList()) {
                    cameraIdList.add(Integer.parseInt(id));
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraIdList;
    }

    @Override
    public List<Size> getSupportPreviewSize() {
        return mSupportPreviewSize;
    }

    @Override
    protected void openCamera() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "openCamera 0 mCameraId=" + mCameraId);
                do {
                    if (null != mCameraDevice) {
                        ImoLog.w(TAG, "openCamera CameraEngine is opened");
                        break;
                    }

                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ImoLog.w(TAG, "openCamera permission denied");
                        break;
                    }

                    final CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                    final Semaphore waiter = new Semaphore(0);
                    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            ImoLog.d(TAG, "openCamera StateCallback onOpened id=" + mCameraId);
                            mCameraDevice = camera;
                            try {
                                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(String.valueOf(mCameraId));
                                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                                mSupportPreviewSize = CameraUtils.convertSizeList(map.getOutputSizes(SurfaceTexture.class));
                                mPreviewSize = CameraUtils.findBestPreviewSizeValue(mPreviewSize, mSupportPreviewSize);
//                                int[] keys = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
//                                for (int i=0;i<keys.length;++i){
//                                    L.i("whb","CONTROL_AWB_AVAILABLE_MODES index:"+i+" value:"+keys[i]);
//                                }

//                        mFocusMode = CameraUtils.findBestFocusModeValue(mFocusMode, parameters);
//                        mFpsRange = CameraUtils.findMaxFpsRangeValue(parameters);
//                        mExposureCompensation = CameraUtils.findBestExposureCompensation(mExposureCompensation, parameters);
//                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//                        Camera.getCameraInfo(mCameraId, cameraInfo);
                                mCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }

                            ImoLog.d(TAG, "openCamera StateCallback cameraId(" + mCameraId + ") success, mPreviewSize=" + mPreviewSize
                                    + ",mFocusMode=" + mFocusMode + ",mFpsRange=" + mFpsRange + ",mCameraOrientation=" + mCameraOrientation
                                    + ",mExposureCompensation=" + mExposureCompensation);
                            synchronized (cameraCallBacks) {
                                for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                    if (cameraCallBack != null) {
                                        cameraCallBack.openCameraSucceed(CameraEngine.this, mCameraId);
                                    }
                                }
                            }

                            if (mSurfaceTexture != null) {
                                startPreview();
                            }
                            waiter.release();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            ImoLog.d(TAG, "openCamera StateCallback onDisconnected id=" + mCameraId);
                            camera.close();
                            mCameraDevice = null;
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            ImoLog.e(TAG, "openCamera StateCallback onError id=" + mCameraId + " error=" + error);
                            camera.close();
                            mCameraDevice = null;

                            synchronized (cameraCallBacks) {
                                for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                    if (cameraCallBack != null) {
                                        cameraCallBack.openCameraError(new Exception("相机打开错误,error=" + error));
                                    }
                                }
                            }
                            waiter.release();
//
                            if (error == ERROR_CAMERA_DEVICE) {
                                ImoLog.e(TAG, "camera Error:error == ERROR_CAMERA_DEVICE");
                                closeCamera();
                                openCamera();
                            }
                        }
                    };

                    try {
                        try {
                            ImoLog.d(TAG, "openCamera cameraManager.openCamera mCameraId=" + mCameraId);
                            cameraManager.openCamera(String.valueOf(mCameraId), stateCallback, new Handler(Looper.getMainLooper()));
                            openedCameraIds.add(mCameraId);
                        } catch (Exception e) {
                            ImoLog.w(TAG, "openCamera cameraId(" + mCameraId + ") open failed, e=" + e);
                            int cameraId = ((mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                                    ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
                            if (openedCameraIds.contains(cameraId)) {
                                throw e;
                            }
                            mCameraId = cameraId;
                            cameraManager.openCamera(String.valueOf(cameraId), stateCallback, new Handler(Looper.getMainLooper()));
                        }
                        waiter.acquire();
                    } catch (Exception e) {
                        ImoLog.e(TAG, "openCamera cameraId(" + mCameraId + ") error, e=" + e);
                        e.printStackTrace();
                        mCameraDevice = null;
                        synchronized (cameraCallBacks) {
                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraError(e);
                                }
                            }
                        }
                    }
                } while (false);
                ImoLog.d(TAG, "openCamera 1");
            }
        });
    }

    @Override
    protected void closeCamera() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                ImoLog.d(TAG, "closeCamera 0");
                stopPreview();
                if (null != mCameraDevice) {
                    try {
                        mCameraDevice.close();
                    } catch (Exception e) {
                        ImoLog.e(TAG, "closeCamera error e=" + e);
                        e.printStackTrace();
                    }
                    mCameraDevice = null;
                }
                isPreviewing = false;
                openedCameraIds.remove(mCameraId);
                ImoLog.d(TAG, "closeCamera 1");
            }
        });
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExposureCompensation(int exposureCompensation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFocusMode(String focusMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getCameraOrientation() {
        return mCameraOrientation;
    }

    @Override
    public int getPreviewFormat() {
        return mPreviewFromat;
    }
}