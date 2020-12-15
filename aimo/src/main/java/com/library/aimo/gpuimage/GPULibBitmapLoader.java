package com.library.aimo.gpuimage;

import android.graphics.Bitmap;

/**
 * Created by whb on 17-9-15.
 */

public class GPULibBitmapLoader {
    private static ImageLoadInterface imageLoader;

    public static void initImageLoader(ImageLoadInterface imageLoaderInterface) {
        imageLoader = imageLoaderInterface;
    }

    public static ImageLoadInterface getInstance() {
        return imageLoader;
    }

    public interface ImageLoadInterface {
        Bitmap loadBitmap(String url);
    }
}
