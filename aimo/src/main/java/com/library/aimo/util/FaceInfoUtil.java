package com.library.aimo.util;

import android.graphics.RectF;

import com.aimall.sdk.facedetector.bean.ImoFaceDetectInfo;
import com.aimall.sdk.trackerdetector.bean.ImoFaceInfo;
import com.library.aimo.bean.FaceRecognitionInfo;

import java.util.List;

public class FaceInfoUtil {

    /**
     * 获取最大头像的那个
     * @param faceInfoLists
     * @return
     */
    public static ImoFaceInfo getBiggestFace(List<ImoFaceInfo> faceInfoLists) {
        ImoFaceInfo faceInfo = null;
        if (faceInfoLists != null && faceInfoLists.size() > 0) {
            faceInfo = faceInfoLists.get(0);
            for (int i = 1; i < faceInfoLists.size(); i++) {
                if (Float.compare(faceInfoLists.get(i).getRect().height(), faceInfo.getRect().height()) > 0) {
                    faceInfo = faceInfoLists.get(i);
                }
            }
        }
        return faceInfo;
    }

    /**
     * 获取人脸的点位信息
     * @param imoFaceDetectInfos
     * @return
     */
    public static float[][] getPointFromImoFaceDetectInfo(List<ImoFaceDetectInfo> imoFaceDetectInfos) {
        if (null == imoFaceDetectInfos) {
            return null;
        }

        float[][] pointss = new float[imoFaceDetectInfos.size()][];
        for (int i = 0; i < imoFaceDetectInfos.size(); i++) {
            pointss[i] = imoFaceDetectInfos.get(i).getPoints();
        }

        return pointss;
    }

    /**
     * 获取人脸的点位信息
     * @param imoFaceDetectInfos
     * @return
     */
    public static float[][] getPointFromImoFaceInfo(List<ImoFaceInfo> imoFaceDetectInfos) {
        if (null == imoFaceDetectInfos) {
            return null;
        }

        float[][] pointss = new float[imoFaceDetectInfos.size()][];
        for (int i = 0; i < imoFaceDetectInfos.size(); i++) {
            pointss[i] = imoFaceDetectInfos.get(i).getPoints();
        }

        return pointss;
    }


    public static float[] getFaceRectFromImoFaceInfo(ImoFaceInfo imoFaceDetectInfos) {
        if (imoFaceDetectInfos != null) {
            return rectToPoints(imoFaceDetectInfos.getRect());
        }
        return null;
    }

    private static float[] rectToPoints(RectF faceRect) {
        float[] floats = new float[4];
        floats[0] = faceRect.left;
        floats[1] = faceRect.top;
        floats[2] = faceRect.width();
        floats[3] = faceRect.height();
        return floats;
    }

    public static boolean IsStraightFace(ImoFaceInfo imoFaceInfo) {
        float confidence = imoFaceInfo.getConfidenceAvg();
//        int yawDegrees = (int) Math.toDegrees(imoFaceInfo.getRotations()[1]);
//        int pitchDegrees = (int) Math.toDegrees(imoFaceInfo.getRotations()[0]);
//        return (Float.compare(confidence, 0.7f) > 0 && (Math.abs(yawDegrees) < 30) && (Math.abs(pitchDegrees) < 20));
        return (Float.compare(confidence, 0.65f) > 0);
    }
}
