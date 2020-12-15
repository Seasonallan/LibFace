package com.library.aimo.bean;

import com.aimall.sdk.extractor.bean.ImoFaceFeature;
import com.aimall.sdk.facedetector.bean.ImoFaceDetectInfo;

/**
 * 人脸信息和对应特帧值的包装类
 */
public class FaceRecognitionInfo {

    // 人脸检测信息
    private ImoFaceDetectInfo imoFaceDetectInfo;
    // 人脸特帧
    private ImoFaceFeature imoFaceFeature;

    public ImoFaceDetectInfo getImoFaceDetectInfo() {
        return imoFaceDetectInfo;
    }

    public void setImoFaceDetectInfo(ImoFaceDetectInfo imoFaceDetectInfo) {
        this.imoFaceDetectInfo = imoFaceDetectInfo;
    }

    public ImoFaceFeature getImoFaceFeature() {
        return imoFaceFeature;
    }

    public void setImoFaceFeature(ImoFaceFeature imoFaceFeature) {
        this.imoFaceFeature = imoFaceFeature;
    }
}
