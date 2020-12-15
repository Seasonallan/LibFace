/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.library.aimo.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.library.aimo.util.ImoLog;
import com.library.aimo.gpuimage.util.TextureRotationUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class GPUImageTwoInputFilter extends GPUImageFilter {
    private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "}";
    private static final String TAG = "GPUImageTwoInputFilter";

    public int mFilterSecondTextureCoordinateAttribute;
    public int mFilterInputTextureUniform2;
    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    protected Buffer mTexture2CoordinatesBuffer;
    protected String mBitmapUrl;

    public GPUImageTwoInputFilter(String fragmentShader) {
        this(VERTEX_SHADER, fragmentShader);
    }

    public GPUImageTwoInputFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
        setRotation(Rotation.NORMAL, false);
    }

    @Override
    public void onInit() {
        super.onInit();

        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        mFilterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);


        setBitmapUrl(mBitmapUrl);
    }


    public void setBitmapUrl(final String bitmapUrl) {
        if (bitmapUrl == null) {
            return;
        }
        mBitmapUrl = bitmapUrl;
        runOnDraw(new Runnable() {
            public void run() {
                if (mFilterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                    final Bitmap bitmap = getBitmapByUrl(mBitmapUrl);
                    if (bitmap == null || bitmap.isRecycled()) {
                        ImoLog.e(TAG, "setBitmapUrl bitmap is null or recycled");
                        return;
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    synchronized (bitmap) {
                        mFilterSourceTexture2 = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
                    }
                }
            }
        });
    }


    public void setBitmap(final Bitmap bitmap) {
        if (bitmap != null && bitmap.isRecycled()) {
            return;
        }
        if (bitmap == null) {
            return;
        }
        runOnDraw(new Runnable() {
            public void run() {
                if (mFilterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                    if (bitmap == null || bitmap.isRecycled()) {
                        return;
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    mFilterSourceTexture2 = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
                }
            }
        });
    }

    public void onDestroy() {
        super.onDestroy();
        OpenGlUtils.deleteTexture(mFilterSourceTexture2);
        mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        super.onDrawArraysPre();
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture2);
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        mTexture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal) {
        float[] buffer = TextureRotationUtil.getRotation(rotation, flipHorizontal, false);

        ByteBuffer bBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        FloatBuffer fBuffer = bBuffer.asFloatBuffer();
        fBuffer.put(buffer);
        fBuffer.flip();

        mTexture2CoordinatesBuffer = bBuffer;
    }


    // 定制第二张纹理图的纹理坐标
    public void setTexture2CoordinatesBuffer(Buffer buffer) {
        mTexture2CoordinatesBuffer = buffer;
    }
}
