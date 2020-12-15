package com.library.aimo.util;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.library.aimo.R;
import com.library.aimo.gpuimage.GPUImageFilter;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * ================================================
 * 作    者：aguai（吴红斌）Github地址：https://github.com/aguai1
 * 版    本：1.0
 * 创建日期：17-12-27
 * 描    述：
 * ================================================
 */
public class FaceRenderer extends GPUImageFilter {

    private boolean isShow = true;
    private List<float[]> mPoints;
    private List<float[]> mRotations;
    private List<float[]> mGazes;
    private List<float[]> mPupils;
    private int mWidth;
    private int mHeight;
    private List<FloatBuffer> faceCubeBuffers = new ArrayList<>();
    private List<FloatBuffer> rotationCubeBuffers = new ArrayList<>();
    private List<FloatBuffer> pupilCubeBuffers = new ArrayList<>();
    private List<FloatBuffer> gazeCubeBuffers = new ArrayList<>();
    private int radiusUniform;
    private int pointColorUniform;

    private ShortBuffer gazeIndicesBuffer;
    private static final short[] gazeIndices = new short[]{0, 1
            , 2, 3
    };

    private float lineWidth;
    private int radius = 3;
    private int _pointVBO;

    public FaceRenderer() {
        super(OpenGlUtils.readShaderFromRawResource(R.raw.lines_vertex),
                OpenGlUtils.readShaderFromRawResource(R.raw.lines_fragment));
        setIsBlend(true);
        lineWidth = 5;
        gazeIndicesBuffer = OpenGlUtils.createShortBuffer(gazeIndices);
    }

    @Override
    public void onInit() {
        super.onInit();
        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        pointColorUniform = GLES20.glGetUniformLocation(getProgram(), "pointColor");

//        int[] tmp = new int[1];
//        GLES20.glGenBuffers(1, tmp, 0);
//        _pointVBO = tmp[0];
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        if (!isShow) {
            return;
        }

        if (!mIsInitialized) {
            return;
        }

        if (mPoints == null) {
            return;
        }
        GLES20.glLineWidth(lineWidth);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mTargetFrameBuffer);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        if (!isBlend()) {
            clearColor();
        }
//        faceCubeBuffer.position(0);
//        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, faceCubeBuffer);
//        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glUniform1f(radiusUniform, radius);
//        GLES20.glUniform4f(pointColorUniform, 0, 1, 0, 1);
        onDrawArraysPre();
//        if (showLine) {
//            GLES20.glDrawElements(GLES20.GL_LINES, drawLength, GLES20.GL_UNSIGNED_SHORT, elements);
//        }

        GLES20.glUniform4f(pointColorUniform, 0, 1, 0, 1);
        for(FloatBuffer faceCubeBuffer : faceCubeBuffers) {
            faceCubeBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, faceCubeBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, _pointVBO);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, faceCubeBuffer.capacity() * BYTES_PER_FLOAT,
//                    faceCubeBuffer, GLES20.GL_STREAM_DRAW);
//            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false,
//                    2 * BYTES_PER_FLOAT, 0);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, faceCubeBuffer.capacity() / 2);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        }

        GLES20.glUniform4f(pointColorUniform, 1, 0, 0, 1);
        GLES20.glUniform1f(radiusUniform, radius * 2f);
        for(FloatBuffer pupilCubeBuffer : pupilCubeBuffers) {
            pupilCubeBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, pupilCubeBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, _pointVBO);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, faceCubeBuffer.capacity() * BYTES_PER_FLOAT,
//                    faceCubeBuffer, GLES20.GL_STREAM_DRAW);
//            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false,
//                    2 * BYTES_PER_FLOAT, 0);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pupilCubeBuffer.capacity() / 2);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        }

        for(FloatBuffer gazeCubeBuffer : gazeCubeBuffers) {
            gazeCubeBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, gazeCubeBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
            GLES20.glDrawElements(GLES20.GL_LINES, gazeIndices.length, GLES20.GL_UNSIGNED_SHORT, gazeIndicesBuffer);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        }

        onDrawArraysAfter();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setFaceInfo(final List<float[]> points, final List<float[]> rotations, final List<float[]> gazes, final List<float[]> pupils, final int width, final int height, final PointF cubeDistance, final PointF textureDistance, final boolean isTranspose) {
        if (!isShow) {
            return;
        }
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mPoints = points;
                mRotations = rotations;
                mGazes = gazes;
                mPupils = pupils;
                mWidth = width;
                mHeight = height;
                updateFaceCubeBuffer(cubeDistance, textureDistance, isTranspose);
            }
        });
    }

    private void updateFaceCubeBuffer(PointF cubeDistance, PointF textureDistance, boolean transpose) {
        faceCubeBuffers.clear();
        gazeCubeBuffers.clear();
        pupilCubeBuffers.clear();
        int width = mWidth;
        int height = mHeight;
        RectF textureCoords = new RectF(textureDistance.x, textureDistance.y, 1f - textureDistance.x, 1f - textureDistance.y);
        if(transpose) {
            width = mHeight;
            height = mWidth;
            textureCoords = new RectF(textureDistance.y, textureDistance.x, 1f - textureDistance.y, 1f - textureDistance.x);
        }

        if (mPoints != null) {
            for(float[] point : mPoints) {
                final int pointCount = point.length / 2;
                FloatBuffer faceCubeBuffer = OpenGlUtils.createFloatBuffer(point.length);
                for (int i = 0; i < pointCount; i++) {
                    float x = point[2 * i];
                    float y = point[2 * i + 1];
                    float xT = x / width;
                    float yT = y / height;
                    float cubeX = (2f * xT - 1f) / textureCoords.width();
                    float cubeY = (1f - 2f * yT) / textureCoords.height();
                    cubeX *= cubeDistance.x;
                    cubeY *= cubeDistance.y;
                    faceCubeBuffer.put(cubeX);
                    faceCubeBuffer.put(cubeY);
                }
                faceCubeBuffer.position(0);
                faceCubeBuffers.add(faceCubeBuffer);
            }
        }
        if(mPupils != null) {
            final float gazeLen = 40.0f;
            for(int k = 0; k < mPupils.size(); k++) {
                float[] pupil = mPupils.get(k);
                if(pupil != null) {
                    final int pupilCount = pupil.length / 2;
                    FloatBuffer pupilCubeBuffer = OpenGlUtils.createFloatBuffer(pupil.length);
                    for (int i = 0; i < pupilCount; i++) {
                        float x = pupil[2 * i];
                        float y = pupil[2 * i + 1];
                        float xT = x / width;
                        float yT = y / height;
                        float cubeX = (2f * xT - 1f) / textureCoords.width();
                        float cubeY = (1f - 2f * yT) / textureCoords.height();
                        cubeX *= cubeDistance.x;
                        cubeY *= cubeDistance.y;
                        pupilCubeBuffer.put(cubeX);
                        pupilCubeBuffer.put(cubeY);
                    }
                    pupilCubeBuffer.position(0);
                    pupilCubeBuffers.add(pupilCubeBuffer);

                    if(mGazes != null && k < mGazes.size()) {
                        float[] gaze = mGazes.get(k);
                        float[] gazePts = new float[]{
                                pupil[0], pupil[1],
                                pupil[0] + gazeLen * gaze[0], pupil[1] + gazeLen * gaze[1],
                                pupil[2], pupil[3],
                                pupil[2] + gazeLen * gaze[3], pupil[3] + gazeLen * gaze[4],
                        };

                        final int pointCount = gazePts.length / 2;
                        FloatBuffer gazeCubeBuffer = OpenGlUtils.createFloatBuffer(gazePts.length);
                        for (int i = 0; i < pointCount; i++) {
                            float x = gazePts[2 * i];
                            float y = gazePts[2 * i + 1];
                            float xT = x / width;
                            float yT = y / height;
                            float cubeX = (2f * xT - 1f) / textureCoords.width();
                            float cubeY = (1f - 2f * yT) / textureCoords.height();
                            cubeX *= cubeDistance.x;
                            cubeY *= cubeDistance.y;
                            gazeCubeBuffer.put(cubeX);
                            gazeCubeBuffer.put(cubeY);
                        }
                        gazeCubeBuffer.position(0);
                        gazeCubeBuffers.add(gazeCubeBuffer);
                    }
                }
            }
        }
    }

    public void show(boolean isShow) {
        this.isShow = isShow;
    }
}
