package com.library.aimo.widget;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//主要的surfaceView，负责展示预览图片，camera的开关
public class CameraView extends SurfaceView {

    // camera 类
    private Camera camera = null;
    private SurfaceHolder holder = null;
    private Camera.Size size;

    public void setCameraSize(Camera.Size size) {
        this.size = size;
    }
    public Camera.Size getCameraSize() {
        return size;
    }

    public CameraView(Context context) {
        super(context);
        holder = this.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                Camera.Parameters parameters = camera.getParameters();
                //size = getPreviewSize(parameters.getSupportedPictureSizes(), parameters.getSupportedPreviewSizes(), height * 1.0f / width);
                if (size != null) {
                    parameters.setPictureSize(size.width, size.height);
                    parameters.setPreviewSize(size.width, size.height);
                }

                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                //parameters.setFocusMode("auto");
                camera.setParameters(parameters);
                camera.startPreview();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (camera == null) {
                    camera = Camera.open();
                }
                try {
                    //设置camera预览的角度，因为默认图片是倾斜90度的
                    camera.setDisplayOrientation(90);
                    //设置holder主要是用于surfaceView的图片的实时预览，以及获取图片等功能，可以理解为控制camera的操作..
                    camera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    camera.release();
                    camera = null;
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        });
//          holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    //最近尺寸
    public static Camera.Size getPreviewSize(List<Camera.Size> listPreview, List<Camera.Size> listPicture, float percent) {
        if (listPicture == null || listPreview == null) {
            return null;
        }
        List<Camera.Size> commonSize = new ArrayList<>();
        for (Camera.Size size : listPreview) {
            for (Camera.Size sizeCheck : listPicture) {
                if (size.width == sizeCheck.width && size.height == sizeCheck.height) {
                    commonSize.add(size);
                }
            }
        }

        float minPercent = Integer.MAX_VALUE;
        int position = 0;
        for (int i = 0; i < commonSize.size(); i++) {
            Camera.Size size = commonSize.get(i);
            float sizePercent = size.width * 1.0f / size.height;
            float dPercent = Math.abs(sizePercent - percent);
            if (dPercent < minPercent) {
                position = i;
                minPercent = dPercent;
            }
        }
        if (commonSize.size() > position) {
            Camera.Size size = commonSize.get(position);
            return size;
        }
        return null;
    }

    //最近尺寸
    public static Camera.Size getCloseSize(List<Camera.Size> list, float percent) {

        float minPercent = Integer.MAX_VALUE;
        int position = 0;
        for (int i = 0; i < list.size(); i++) {
            Camera.Size size = list.get(i);
            float sizePercent = size.width * 1.0f / size.height;
            float dPercent = Math.abs(sizePercent - percent);
            if (dPercent < minPercent) {
                position = i;
                minPercent = dPercent;
            }
        }
        if (list.size() > position) {
            Camera.Size size = list.get(position);
            return size;
        }
        return null;
    }

    public void restart() {
        try {
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takePicture(Camera.PictureCallback picture) {
        try {
            camera.takePicture(null, null, picture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindCamera(Camera camera) {
        this.camera = camera;
    }
}
