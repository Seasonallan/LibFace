package com.library.aimo.gpuimage.gl.output.picture;

import android.graphics.Bitmap;

/**
 * Created by ian on 2017/9/8.
 */


public interface TakePictureCallback {
    void onTakingPicture(final Bitmap bitmap);
}