package com.library.aimo.core;

public interface CameraCallBack {
    void openCameraError(Exception e);
    void openCameraSucceed(BaseCameraEngine cameraEngine, int cameraId);
    void onPreviewFrame(BaseCameraEngine cameraEngine, byte[] data);
}