package com.library.aimo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.library.aimo.R;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.util.Constants;
import com.library.aimo.util.DrawInterface;
import com.library.aimo.util.FaceRenderer;
import com.library.aimo.util.FaceRenderer3d;
import com.library.aimo.util.FaceRendererRotation;
import com.library.aimo.util.MutiLayerFilter;
import com.library.aimo.util.OpenGlUtils;
import com.library.aimo.core.BaseCameraEngine;
import com.library.aimo.core.CameraCallBack;
import com.library.aimo.core.CameraUtils;
import com.library.aimo.core.Size;
import com.library.aimo.gpuimage.GPUImage;
import com.library.aimo.gpuimage.GPUImageFilter;
import com.library.aimo.gpuimage.GPUImageView;

import java.io.File;
import java.util.List;

/**
 * ================================================
 * 作    者：aguai（吴红斌）Github地址：https://github.com/aguai1
 * 版    本：1.0
 * 创建日期：17-12-6
 * 描    述：相机控件
 * ================================================
 */
public class CameraContainer extends FrameLayout implements CameraCallBack {
    private GPUImageView gpuImageView;
    private ImageView switchBtn;
    private BaseCameraEngine cameraEngine;
    private View takePicBtn;
    private DrawInfoView drawInfoView;

    private FaceRenderer faceRenderer = new FaceRenderer();
    private FaceRenderer3d faceRenderer3d = new FaceRenderer3d();
    private FaceRendererRotation faceRendererRotation = new FaceRendererRotation();

    private boolean openBackCamera;
    private int scaleType;
    private boolean camera2API;
    private boolean showDrawPointsView;

    private boolean refreshCanvas;

    private int cameraRotateAdjust;
    private boolean flipX = false;

    private static final int SCALE_TYPE_CENTER_CROP = 0;
    private static final int SCALE_TYPE_CENTER_INSIDE = 1;
    private TakePicCallBack onTakePicCallback;

    public CameraContainer(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CameraContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RendererView, 0, 0);
        scaleType = ta.getInt(R.styleable.RendererView_scaleType, SCALE_TYPE_CENTER_CROP);
        ta.recycle();
        ta = context.obtainStyledAttributes(attrs, R.styleable.CameraContainer, 0, 0);
        openBackCamera = ta.getBoolean(R.styleable.CameraContainer_openBackCamera, false);
        camera2API = ta.getBoolean(R.styleable.CameraContainer_camera2API, false);
        ta.recycle();
        View inflate = LayoutInflater.from(context).inflate(R.layout.include_camera_container, this, true);
        gpuImageView = inflate.findViewById(R.id.gpuImageView);

        switchBtn = inflate.findViewById(R.id.iv_switch);
        switchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraEngine.switchCamera();
            }
        });

        takePicBtn = inflate.findViewById(R.id.iv_take_pic);
        takePicBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = takePic();
                File file = BitmapUtils.savePhotoToSDCard(bitmap, Constants.getBitmapPath(), "IMG_" + System.currentTimeMillis() + ".jpg");
                if (file != null) {
                    if (onTakePicCallback != null) {
                        onTakePicCallback.OnTakePic(bitmap, file.getAbsolutePath());
                    }
                } else {
                    BitmapUtils.recycleBitmap(bitmap);
                }

//                gpuImageView.getGPUImage().capturePicture(new ResultFrameCallback() {
//                    @Override
//                    public void onResultFrame(Bitmap bitmap) {
//                        try {
//                            String path = BitmapUtils.saveBitmap(getContext(), bitmap);
//                            if (onTakePicCallback!=null){
//                                onTakePicCallback.OnTakePic(bitmap,path);
//                            }else {
//                                BitmapUtils.recycleBitmap(bitmap);
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            BitmapUtils.recycleBitmap(bitmap);
//                        }
//                    }
//                });
            }
        });

        drawInfoView = inflate.findViewById(R.id.drawPointView);

        if (camera2API && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraEngine = new com.library.aimo.camera2.CameraEngine(getContext());
        } else {
            cameraEngine = new com.library.aimo.camera1.CameraEngine(getContext());
        }
        cameraEngine.addCameraCallBack(this);
        cameraEngine.setCameraId(openBackCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);

        showSwitchCamera(false);

        boolean isCenterCrop = (scaleType == SCALE_TYPE_CENTER_CROP);
        gpuImageView.setScaleType((isCenterCrop ? GPUImage.ScaleType.CENTER_CROP : GPUImage.ScaleType.CENTER_INSIDE));
        drawInfoView.setCenterCrop(isCenterCrop);

        MutiLayerFilter mutiLayerFilter = new MutiLayerFilter(new GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, OpenGlUtils.readShaderFromRawResource(R.raw.opaque_fragment)));
        mutiLayerFilter.addFilter(faceRenderer);
        mutiLayerFilter.addFilter(faceRenderer3d);
        mutiLayerFilter.addFilter(faceRendererRotation);
        gpuImageView.setFilter(mutiLayerFilter);
        faceRenderer3d.show(false);
    }

    @Override
    public void openCameraSucceed(final BaseCameraEngine cameraEngine, int cameraId) {
        cameraEngine.startPreview();
//        post(new Runnable() {
//            @Override
//            public void run() {
//                PopContainer.PopupWindowBuilder popupWindowBuilder = new PopContainer.PopupWindowBuilder(getContext(), textChangeQuality);
//                for (final Size size : cameraEngine.getSupportPreviewSize()) {
//                    popupWindowBuilder.addItem(size.width + "x" + size.height, true, new OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            cameraEngine.switchPreviewSize(size);
//                        }
//                    });
//                }
//                popContainer = popupWindowBuilder.create();
//            }
//        });
    }

    @Override
    public void openCameraError(Exception e) {

    }

    public PointF getTextureDistance() {
        return gpuImageView.getGPUImage().getTextureDistance();
    }

    @Override
    public void onPreviewFrame(BaseCameraEngine baseCameraEngine, byte[] data) {
        Size previewSize = cameraEngine.getPreviewSize();
        // 特殊设备手动适配
        int cameraRotate = cameraEngine.getCameraRotate();
        cameraRotate += cameraRotateAdjust;
        cameraRotate += 360;
        cameraRotate %= 360;
        boolean flipHorizontal = cameraEngine.isFrontCamera();
        if (flipX) flipHorizontal = !flipHorizontal;

        gpuImageView.getGPUImage().onPreviewFrameNv21(data, previewSize.width, previewSize.height, cameraRotate, flipHorizontal);

    }

    @Deprecated
    public BaseCameraEngine getCameraEngine() {
        return cameraEngine;
    }

    public void setFilter(GPUImageFilter filter) {
        MutiLayerFilter mutiLayerFilter = new MutiLayerFilter();
        mutiLayerFilter.addFilter(filter);
        mutiLayerFilter.addFilter(faceRenderer);
        mutiLayerFilter.addFilter(faceRenderer3d);
        mutiLayerFilter.addFilter(faceRendererRotation);
        gpuImageView.setFilter(mutiLayerFilter);
    }

    public void addCameraCallBack(CameraCallBack cameraCallBack) {
        cameraEngine.addCameraCallBack(cameraCallBack);
    }

    public void removeCameraCallBack(CameraCallBack cameraCallBack) {
        cameraEngine.removeCameraCallBack(cameraCallBack);
    }

    public void setPreviewSize(int width, int height) {
        cameraEngine.setPreviewSize(new Size(width, height));
    }

    public void setCameraId(int cameraId) {
        cameraEngine.setCameraId(cameraId);
    }

    public void runOnCameraThread(Runnable runnable) {
        cameraEngine.runOnCameraThread(runnable);
    }

    public Size getPreviewSize() {
        return cameraEngine.getPreviewSize();
    }

    public int getCameraRotate() {
        return cameraEngine.getCameraRotate();
    }

    public boolean isFrontCamera() {
        return cameraEngine.isFrontCamera();
    }

    /**
     * 强制刷新上层绘制
     */
    public void refreshCanvasDraw() {
        Size previewSize = cameraEngine.getPreviewSize();
        int width = previewSize.width;
        int height = previewSize.height;
        if (gpuImageView.getGPUImage().isTranspose()) {
            width = previewSize.height;
            height = previewSize.width;
        }
        drawInfoView.postInvalidate(width, height);
    }

    public void showSwitchCamera(boolean show) {
        if (show && cameraEngine.getSupportCameraId().size() > 1) {
            switchBtn.setVisibility(VISIBLE);
        } else {
            switchBtn.setVisibility(GONE);
        }
    }


    public void showPoints(List<float[]> shape) {
        showPoints(shape, null, null, null, null, null, null);
    }

    public void showPoints(List<float[]> shape, List<float[]> rotations, List<float[]> gaze, List<float[]> pupils, List<float[]> shape3d, List<RectF> translateInImage, List<Float> scales) {
        PointF cubeDistance = gpuImageView.getGPUImage().getCubeDistance();
        PointF textureDistance = gpuImageView.getGPUImage().getTextureDistance();
        boolean isTranspose = gpuImageView.getGPUImage().isTranspose();
        Size previewSize = cameraEngine.getPreviewSize();

        faceRenderer.setFaceInfo(shape, rotations, gaze, pupils, previewSize.width, previewSize.height, cubeDistance, textureDistance, isTranspose);
        faceRenderer3d.setFaceInfo(shape3d, shape, translateInImage, scales, previewSize.width, previewSize.height, cubeDistance, textureDistance, isTranspose);
        faceRendererRotation.setFaceInfo(shape, rotations, scales, previewSize.width, previewSize.height, cubeDistance, textureDistance, isTranspose);

        if (refreshCanvas) {
            int width = previewSize.width;
            int height = previewSize.height;
            if (isTranspose) {
                width = previewSize.height;
                height = previewSize.width;
            }
            drawInfoView.postInvalidate(width, height);
        }
    }

    public Bitmap takePic() {
        try {
            return gpuImageView.capture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void showTakePic(boolean show) {
        takePicBtn.setVisibility(show ? VISIBLE : GONE);
    }

    @UiThread
    public void showUserLogInfo(final String userInfo) {
//        post(new Runnable() {
//            @Override
//            public void run() {
//                tvInfo.setText(userInfo);
//            }
//        });
    }

    public void onResume() {
        cameraEngine.onResume();
    }

    public void onPause() {
        cameraEngine.onPause();
    }

    public void onDestroy() {
        if (cameraEngine != null) {
            cameraEngine.removeCameraCallBack(this);
            cameraEngine.onPause();
            cameraEngine = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (cameraEngine != null)
            cameraEngine.removeCameraCallBack(this);
        super.onDetachedFromWindow();
        if (cameraEngine != null)
            cameraEngine.onPause();
        cameraEngine = null;
    }

    public void refreshConfig(UiConfig uiConfig) {
        showLog(uiConfig.showLog);
        showImageQuality(uiConfig.showChangeImageQuality);
        showTakePic(uiConfig.showTakePic);
        this.onTakePicCallback = uiConfig.onTakePicCallback;
        showSwitchCamera(uiConfig.showSwitchCamera);

        this.showDrawPointsView = uiConfig.showDrawPointsView;
        this.refreshCanvas = uiConfig.refreshCanvas;
        this.cameraRotateAdjust = uiConfig.cameraRotateAdjust;
        this.flipX = uiConfig.flipX;
        drawInfoView.setDrawInterface(uiConfig.cameraDrawInterface);
    }

    private void showLog(boolean show) {
//        tvInfo.setVisibility(show ? VISIBLE : GONE);
    }

    private void showImageQuality(boolean show) {
//        textChangeQuality.setVisibility(show ? VISIBLE : GONE);
    }

    public void requestRender() {
        gpuImageView.requestRender();
    }

    public interface TakePicCallBack {
        void OnTakePic(Bitmap bitmap, String path);
    }

    public void showPts3d(boolean isShow) {
        faceRenderer3d.show(isShow);
    }

    public static class UiConfig {
        private boolean showTakePic = true;
        private boolean showDrawPointsView = true;
        private DrawInterface cameraDrawInterface;
        private boolean showSwitchCamera = false;
        private boolean showChangeImageQuality = true;
        private boolean showLog = true;
        private boolean refreshCanvas = true;
        public int cameraRotateAdjust = 0;
        public boolean flipX = false;
        private TakePicCallBack onTakePicCallback;

        /**
         * 显示拍照按钮
         *
         * @param show
         * @return
         */
        public UiConfig showTakePic(boolean show) {
            showTakePic = show;
            return this;
        }

        public UiConfig refreshCanvasWhenPointRefresh(boolean show) {
            refreshCanvas = show;
            return this;
        }

        /**
         * 显示拍照按钮切换分辨率
         *
         * @param show
         * @return
         */
        public UiConfig showChangeImageQuality(boolean show) {
            showChangeImageQuality = show;
            return this;
        }

        public UiConfig showSwitchCamera(boolean show) {
            showSwitchCamera = show;
            return this;
        }

        public UiConfig showLog(boolean show) {
            showLog = show;
            return this;
        }

        public UiConfig setOnTakePicCallback(TakePicCallBack onTakePicCallback) {
            this.onTakePicCallback = onTakePicCallback;
            return this;
        }

        /**
         * 画点
         *
         * @param show
         * @return
         */
        public UiConfig showDrawPointsView(boolean show) {
            showDrawPointsView = show;
            return this;
        }

        /**
         * 自定义绘制
         *
         * @param cameraDrawInterface
         * @return
         */
        public UiConfig drawInfoCanvas(DrawInterface cameraDrawInterface) {
            this.cameraDrawInterface = cameraDrawInterface;
            return this;
        }

        public UiConfig setCameraRotateAdjust(int cameraRotateAdjust) {
            this.cameraRotateAdjust = cameraRotateAdjust;
            return this;
        }

        public UiConfig setFlipX(boolean flipX) {
            this.flipX = flipX;
            return this;
        }
    }
}