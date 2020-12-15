package com.library.aimo.api;

import android.graphics.RectF;

import com.library.aimo.core.BaseCameraEngine;

/**
 * 定义外部调用的接口
 */
public interface IFaceAction {
    /**
     * 初始化，载入so
     */
    void init();

    /**
     * 相机预览流处理
     * @param cameraEngine
     * @param bytes
     * @return
     */
    RectF onPreView(BaseCameraEngine cameraEngine, byte[] bytes);

    /**
     * 资源回收
     */
    void onDestroy();
}
