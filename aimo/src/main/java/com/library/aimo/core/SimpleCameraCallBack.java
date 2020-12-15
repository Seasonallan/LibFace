package com.library.aimo.core;

public abstract class SimpleCameraCallBack implements CameraCallBack {
    @Override
    public void openCameraError(Exception e) {

    }

    @Override
    public void openCameraSucceed(BaseCameraEngine cameraEngine, int cameraId) {

    }

    @Override
    public void onPreviewFrame(BaseCameraEngine cameraEngine, byte[] data) {

    }
}