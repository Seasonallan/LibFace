package com.library.aimo.api;

import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.aimall.sdk.extractor.ImoFaceExtractor;
import com.aimall.sdk.trackerdetector.bean.ImoFaceInfo;
import com.library.aimo.bean.FaceRecognitionInfo;
import com.library.aimo.config.SharedPreferencesUtils;

import java.util.List;

public class StaticOpenApi {

    /**
     * 计算两个人脸特征的相似度
     *
     * @return 相似度得分 0~1
     */
    public static float compare(@NonNull float[] features1, @NonNull float[] features2) {
        return ImoFaceExtractor.compare(features1, features2);
    }


    /**
     * 获取图片的人脸信息以及特征值
     *
     * @param bitmap
     * @return
     */
    public static float[] getBitmapFeature(Bitmap bitmap) {
        List<FaceRecognitionInfo> fromList = IMoRecognitionManager.getInstance().execBitmap(bitmap);
        FaceRecognitionInfo faceInfo = ((null != fromList && !fromList.isEmpty()) ? fromList.get(0) : null);
        if (null != faceInfo) {
            return faceInfo.getImoFaceFeature().getFeatures();
        }
        return null;
    }


    public static RectF getBitmapRect(Bitmap bitmap) {
        List<FaceRecognitionInfo> fromList = IMoRecognitionManager.getInstance().execBitmap(bitmap);
        FaceRecognitionInfo faceInfo = ((null != fromList && !fromList.isEmpty()) ? fromList.get(0) : null);
        if (null != faceInfo) {
            return faceInfo.getImoFaceDetectInfo().getRect();
        }
        return null;
    }

    /**
     * 判断人脸图片相似度
     *
     * @param feature
     * @param bitmap
     * @return
     */
    public static int findMatchResults(float[] feature, Bitmap bitmap) {
        float maxScore = -1;
        List<FaceRecognitionInfo> faceRecognitionInfos = IMoRecognitionManager.getInstance().getFaceRecognitionInfos(bitmap);
        if (faceRecognitionInfos != null && faceRecognitionInfos.size() > 0) {
            for (FaceRecognitionInfo faceRecognitionInfo : faceRecognitionInfos) {
                if (null != faceRecognitionInfo.getImoFaceFeature()) {
                    float[] faceInfoFeatures = faceRecognitionInfo.getImoFaceFeature().getFeatures();
                    if (faceInfoFeatures != null) {
                        float score = compare(faceInfoFeatures, feature);
                        maxScore = Math.max(maxScore, score);
                    }
                }
            }
        }
        return (int) (maxScore * 100);
    }

    /**
     * 本地是否存在人脸特征
     *
     * @param id
     * @return
     */
    public static boolean existLocalFace(String id) {
        return SharedPreferencesUtils.contains(id);
    }

    /**
     * 获取本地保存的人脸特征
     *
     * @param id
     * @return
     */
    public static float[] getLocalFace(String id) {
        try {

            if (SharedPreferencesUtils.contains(id)) {
                String localFeature = (String) SharedPreferencesUtils.get(id, "");
                String[] split = localFeature.split(",");
                float[] res = new float[split.length];
                for (int i = 0; i < split.length; i++) {
                    res[i] = Float.parseFloat(split[i]);
                }
                return res;
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 保存人脸特征到本地
     *
     * @param id
     * @param feature
     */
    public static void saveLocalFace(String id, float[] feature) {
        if (feature != null) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < feature.length; i++) {
                stringBuffer.append(feature[i]);
                if (i != feature.length - 1) {
                    stringBuffer.append(",");
                }
            }
            SharedPreferencesUtils.put(id, stringBuffer.toString());
        }
    }

}
