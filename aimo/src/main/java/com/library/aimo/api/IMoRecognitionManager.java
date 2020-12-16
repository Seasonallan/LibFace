package com.library.aimo.api;

import android.graphics.Bitmap;

import androidx.annotation.WorkerThread;

import com.aimall.core.ImoErrorCode;
import com.aimall.core.define.ImoImageFormat;
import com.aimall.core.define.ImoImageOrientation;
import com.aimall.sdk.extractor.ImoFaceExtractor;
import com.aimall.sdk.extractor.bean.ImoFaceFeature;
import com.aimall.sdk.facedetector.ImoFaceDetector;
import com.aimall.sdk.facedetector.bean.ImoFaceDetectInfo;
import com.aimall.sdk.faceliveness.ImoFaceLiveness;
import com.aimall.sdk.trackerdetector.ImoFaceTrackerDetector;
import com.aimall.sdk.trackerdetector.bean.ImoFaceInfo;
import com.library.aimo.bean.FaceRecognitionInfo;
import com.library.aimo.config.SettingConfig;
import com.library.aimo.util.BitmapUtils;
import com.library.aimo.util.FaceInfoUtil;
import com.library.aimo.util.ImoLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class IMoRecognitionManager {

    // 单例实现
    private static IMoRecognitionManager sInstance = new IMoRecognitionManager();

    public static IMoRecognitionManager getInstance() {
        return sInstance;
    }

    private IMoRecognitionManager() {
    }

    //活体检测
    private ImoFaceLiveness imoFaceLiveness;
    private ImoFaceTrackerDetector imoFaceTracker;

    // 人脸特帧提取
    private ImoFaceExtractor imoFaceExtractor;
    // 人脸检测
    private ImoFaceDetector imoFaceDetector;

    // 相机流场景人脸识别时的回调方法
    private AsyncFrameCallback asyncFrameCallback;


    // 是否正在异步提取特帧值，用于相机流连续提取特帧值的场景
    private boolean isAsyncExtracting = false;


    /**
     * 初始化
     *
     * @param algrothmNumThread 算法线程数
     * @param initListener      初始化监听器
     */
    public synchronized void init(int algrothmNumThread, InitListener initListener) {
        ImoLog.d("init algrothmNumThread=" + algrothmNumThread);

        releaseImpl();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int errorCode;
                String errorMsg = null;
                do {
                    ImoFaceExtractor imoFaceExtractor = new ImoFaceExtractor();
                    errorCode = imoFaceExtractor.init();
                    if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
                        errorMsg = "imoFaceExtractor.init失败！！！errorCode=" + errorCode;
                        errorCode += 1000;
                        break;
                    }

                    ImoFaceDetector imoFaceDetector = new ImoFaceDetector()
                            .configNumThreads(algrothmNumThread);
                    errorCode = imoFaceDetector.init();
                    if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
                        errorMsg = "imoFaceDetector.init失败！！！errorCode=" + errorCode;
                        errorCode += 2000;
                        imoFaceExtractor.destroy();
                        break;
                    }


                    ImoFaceLiveness imoFaceLiveness = new ImoFaceLiveness()
                            .configNumThreads(SettingConfig.getAlgorithmNumThread());
                    errorCode = imoFaceLiveness.init();
                    if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
                        errorMsg = "imoFaceLiveness.init失败！！！errorCode=" + errorCode;
                        errorCode += 2000;
                        imoFaceLiveness.destroy();
                        break;
                    }
                    ImoFaceTrackerDetector imoFaceTracker = new ImoFaceTrackerDetector();
                    errorCode = imoFaceTracker.init();
                    if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
                        errorMsg = "imoFaceTracker.init失败！！！errorCode=" + errorCode;
                        errorCode += 2000;
                        imoFaceTracker.destroy();
                        break;
                    }
                    imoFaceTracker.setModel(ImoFaceTrackerDetector.Model.IMO_FACE_TRACKER_DETECTOR_MODEL_VIDEO);

                    IMoRecognitionManager.this.imoFaceExtractor = imoFaceExtractor;
                    IMoRecognitionManager.this.imoFaceDetector = imoFaceDetector;
                    IMoRecognitionManager.this.imoFaceLiveness = imoFaceLiveness;
                    IMoRecognitionManager.this.imoFaceTracker = imoFaceTracker;
                } while (false);

                if (errorCode != ImoErrorCode.IMO_API_RET_SUCCESS) {
                    if (null != initListener) {
                        initListener.onError(errorCode, errorMsg);
                    }
                } else {
                    if (null != initListener) {
                        initListener.onSucceed();
                    }
                }

                ImoFaceDetector.setPerformanceListener((all, core, preprocess) ->
                        ImoLog.v("onPerformanceCallback duration all=" + all + " core=" + core + " preprocess=" + preprocess));
            }
        }).start();
    }

    /**
     * 销毁
     */
    public synchronized void release() {
        releaseImpl();
    }


    /**
     * 获取bitmap中人脸信息以及特征值，用于底库录入和图片比对
     */
    public synchronized ArrayList<FaceRecognitionInfo> execBitmap(Bitmap bitmap) {
        ArrayList<FaceRecognitionInfo> faceRecognitionInfoLists = new ArrayList<>();
        if (null != imoFaceDetector && null != imoFaceExtractor) {
            List<ImoFaceDetectInfo> imoFaceInfoLists = imoFaceDetector.execBitmap(bitmap, ImoImageOrientation.IMO_IMAGE_UP);
            if (imoFaceInfoLists.size() != 0) {
                float[][] points = FaceInfoUtil.getPointFromImoFaceDetectInfo(imoFaceInfoLists);
                ImoFaceFeature[] imoFaceFeatures = imoFaceExtractor.execBitmap(bitmap, points);
                faceRecognitionInfoLists = generateFaceRecognitionInfoLists(imoFaceInfoLists, imoFaceFeatures);
            }
        }
        return faceRecognitionInfoLists;
    }


    public List<FaceRecognitionInfo> getFaceRecognitionInfoLists(Bitmap bitmap) {
        // 先提取当前这一帧图像的人脸信息
        List<ImoFaceDetectInfo> imoFaceInfoLists = imoFaceDetector.execBitmap(bitmap, ImoImageOrientation.IMO_IMAGE_UP);
        if (imoFaceInfoLists != null && imoFaceInfoLists.size() > 0) {
            // 人脸信息转化为float二维数组，因为提取特帧值需要用到人脸的点位信息
            float[][] points = FaceInfoUtil.getPointFromImoFaceDetectInfo(imoFaceInfoLists);
            ImoFaceFeature[] imoFaceFeatures = imoFaceExtractor.execBitmap(bitmap, points);
            return generateFaceRecognitionInfoLists(imoFaceInfoLists, imoFaceFeatures);
        }
        return null;
    }


    /**
     * 数据流 转化为 bitmap 图片
     *
     * @param nv21Data
     * @param width
     * @param height
     * @param format
     * @param orientation
     * @param flip
     * @return
     */
    public Bitmap bytes2bitmap(byte[] nv21Data, int width, int height, ImoImageFormat format, ImoImageOrientation orientation, boolean flip) {
        //转换为bitmap，可以自行根据faceRect进行头像裁剪等操作
        if (imoFaceLiveness == null) {
            return null;
        }
        //转换为bitmap，可以自行根据faceRect进行头像裁剪等操作
        Bitmap bitmap = imoFaceLiveness.convertYuvToBitmap(nv21Data, width, height, format);
        //旋转图片
        bitmap = BitmapUtils.rotateBitmap(bitmap, -orientation.value(), true);
        if (flip) {
            // 前置相机翻转照片
            bitmap = BitmapUtils.getFilpBitmap(bitmap, true);
        }
        return bitmap;
    }


    private boolean faceMatchedOnce = false;
    public synchronized void execFrameBytes(byte[] data, int width, int height, ImoImageFormat format, ImoImageOrientation orientation, boolean flip, int cameraRotate, boolean checkScore) {
        // 如果有人脸并且当前没有在做异步特帧提取时
        if (!isAsyncExtracting) {
            isAsyncExtracting = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    {
                        int faceDisappear = -1;
                        if (imoFaceTracker != null && imoFaceLiveness != null && imoFaceExtractor != null) {
                            //第一步：获取数据流的所有人脸信息 矩阵和五官位置
                            List<ImoFaceInfo> imoFaceInfoList = imoFaceTracker.execBytes(data, width, height, format, orientation);
                            if (imoFaceInfoList.size() > 0) {
                                ImoFaceInfo biggestFace = FaceInfoUtil.getBiggestFace(imoFaceInfoList);
                                if (FaceInfoUtil.IsStraightFace(biggestFace)) {
                                    float[] faceRect = FaceInfoUtil.getFaceRectFromImoFaceInfo(biggestFace);
                                    //第二步：获取头像位置的人脸的分数
                                    float[] score = imoFaceLiveness.exec(data, width, height, format, orientation, faceRect);

                                    if (checkScore){//分数大于0.97，代表是人脸》》 开始跟本地特征值对比
                                        if (score.length > 0 && score[0] > 0.97){
                                            float[][] points = FaceInfoUtil.getPointFromImoFaceInfo(imoFaceInfoList);
                                            //第三步：获取人脸的特征值
                                            ImoFaceFeature[] imoFaceFeatures = imoFaceExtractor.execBytes(data, width, height, format, points);
                                            if (null != asyncFrameCallback) {
                                                asyncFrameCallback.onFaceMatch(data, width, height, format, orientation, flip, cameraRotate, imoFaceFeatures, score[0]);
                                                faceMatchedOnce = true;
                                                faceDisappear = -1;
                                            }
                                        }
                                    }else{
                                        if (null != asyncFrameCallback) {
                                            asyncFrameCallback.onFaceMatch(data, width, height, format, orientation, flip, cameraRotate, null, score[0]);
                                            faceMatchedOnce = true;
                                            faceDisappear = -1;
                                        }
                                    }
                                }else{
                                    faceDisappear = 2;
                                }
                            }else{
                                faceDisappear = 1;
                            }
                        }
                        if (faceDisappear > 0 && null != asyncFrameCallback && faceMatchedOnce) {
                            ImoLog.e("faceDisappear>> "+ faceDisappear);
                            asyncFrameCallback.onFaceDisappear();
                        }
                        isAsyncExtracting = false;
                    }
                }
            }).start();
        }
    }


    // 设置提取人脸特帧值结束回调，用于相机流连续提取特帧值的场景
    public synchronized void setAsyncFrameCallback(AsyncFrameCallback asyncFrameCallback) {
        this.asyncFrameCallback = asyncFrameCallback;
    }


    // 销毁
    private void releaseImpl() {
        if (imoFaceExtractor != null) {
            imoFaceExtractor.destroy();
            imoFaceExtractor = null;
        }
        if (imoFaceDetector != null) {
            imoFaceDetector.destroy();
            imoFaceDetector = null;
        }
        if (imoFaceLiveness != null) {
            imoFaceLiveness.destroy();
            imoFaceLiveness = null;
        }
        if (imoFaceTracker != null) {
            imoFaceTracker.destroy();
            imoFaceTracker = null;
        }
    }


    private static ArrayList<FaceRecognitionInfo> generateFaceRecognitionInfoLists(List<ImoFaceDetectInfo> imoFaceInfoLists, ImoFaceFeature[] imoFaceFeatures) {
        ArrayList<FaceRecognitionInfo> faceRecognitionInfoLists = new ArrayList<>();
        if (null != imoFaceInfoLists) {
            for (int i = 0; i < imoFaceInfoLists.size(); ++i) {
                FaceRecognitionInfo faceRecognitionInfo = new FaceRecognitionInfo();
                faceRecognitionInfo.setImoFaceDetectInfo(imoFaceInfoLists.get(i));
                if (null != imoFaceFeatures && imoFaceFeatures.length > 0) {
                    faceRecognitionInfo.setImoFaceFeature(imoFaceFeatures[0]);
                }
                faceRecognitionInfoLists.add(faceRecognitionInfo);
            }
        }
        return faceRecognitionInfoLists;
    }


    @WorkerThread
    public interface AsyncFrameCallback {
        void onFaceDisappear();
        void onFaceMatch(byte[] data, int width, int height, ImoImageFormat format, ImoImageOrientation orientation, boolean flip, int cameraRotate, ImoFaceFeature[] faceInfoLists, float score);
    }

    public interface InitListener {
        void onSucceed();

        void onError(int code, String msg);
    }
}
