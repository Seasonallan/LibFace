package com.library.aimo.gpuimage;

public interface IGLView {
    void requestRender();

    void queueEvent(Runnable runnable);

    void setRenderer(GPUImageRenderer mRenderer);
}
