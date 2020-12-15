package com.library.aimo.camera2;

import android.media.Image;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

public class ImageUtil {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] image2Yuv420888ToMat(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        int ySize = yPlane.getBuffer().remaining();

        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        // be aware that this size does not include the padding at the end, if there is any
        // (e.g. if pixel stride is 2 the size is ySize / 2 - 1)
        int uSize = uPlane.getBuffer().remaining();
        int vSize = vPlane.getBuffer().remaining();

        byte[] data = new byte[ySize + (ySize/2)];

        yPlane.getBuffer().get(data, 0, ySize);

        ByteBuffer ub = uPlane.getBuffer();
        ByteBuffer vb = vPlane.getBuffer();

        int uvPixelStride = uPlane.getPixelStride(); //stride guaranteed to be the same for u and v planes
        if (uvPixelStride == 1) {
            uPlane.getBuffer().get(data, ySize, uSize);
            vPlane.getBuffer().get(data, ySize + uSize, vSize);
            return data;
        }

        // if pixel stride is 2 there is padding between each pixel
        // converting it to NV21 by filling the gaps of the v plane with the u values
        vb.get(data, ySize, vSize);
        for (int i = 0; i < uSize; i += 2) {
            data[ySize + i + 1] = ub.get(i);
        }
        return data;
    }

}
