package com.library.aimo.util;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.library.aimo.R;
import com.library.aimo.gpuimage.GPUImageFilter;

import java.nio.FloatBuffer;
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
public class FaceRenderer3d extends GPUImageFilter {

    private boolean isShow = true;
    private List<float[]> mPoints;
    private List<float[]> mPoints2d;
    private List<RectF> mTranslateInImages;
    private List<Float> mScales;
    private int mWidth;
    private int mHeight;
    private boolean mIsTranspose;
    private PointF mTextureDistance;
    private PointF mCubeDistance;
    private List<FloatBuffer> faceCubeBuffers = new ArrayList<>();
    private int radiusUniform;
    private int pointColorUniform;
    private int mvpMatrixUniform;

    private float[] mMvpMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    private float[] mModelViewMatrix = new float[16];

    private float rotateY = 0f;

    private float lineWidth;
    private int radius = 3;
    private int _pointVBO;

    public FaceRenderer3d() {
        super(OpenGlUtils.readShaderFromRawResource(R.raw.pts3d_vertex),
                OpenGlUtils.readShaderFromRawResource(R.raw.lines_fragment));
        setIsBlend(true);
        lineWidth = 5;
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -5, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void onInit() {
        super.onInit();
        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        pointColorUniform = GLES20.glGetUniformLocation(getProgram(), "pointColor");
        mvpMatrixUniform = GLES20.glGetUniformLocation(getProgram(), "mvpMatrix");

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

        onDrawArraysPre();

        int width = mWidth;
        int height = mHeight;
        if(mIsTranspose) {
            width = mHeight;
            height = mWidth;
        }

        GLES20.glUniform1f(radiusUniform, radius);
        GLES20.glUniform4f(pointColorUniform, 1, 1, 1, 1);
        for(int i = 0; i < faceCubeBuffers.size(); i++) {
            FloatBuffer faceCubeBuffer = faceCubeBuffers.get(i);
            faceCubeBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition, 3, GLES20.GL_FLOAT, false, 0, faceCubeBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, _pointVBO);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, faceCubeBuffer.capacity() * BYTES_PER_FLOAT,
//                    faceCubeBuffer, GLES20.GL_STREAM_DRAW);
//            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false,
//                    2 * BYTES_PER_FLOAT, 0);

            float[] point2d = mPoints2d.get(i);
//            float[] translateInImage = new float[]{point2d[27 * 2] - (point2d[33 * 2] - point2d[27 * 2]), point2d[27 * 2 + 1] - (point2d[33 * 2 + 1] - point2d[27 * 2 + 1])};
            RectF rectF = mTranslateInImages.get(i);
            float[] translateInImage = {rectF.centerX(), rectF.centerY()};
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, translateInImage[0] - width / 2, translateInImage[1] - height * 3 / 4, 0);
            Matrix.rotateM(mModelMatrix, 0, rotateY, 0, 1, 0);

            Matrix.setIdentityM(mModelViewMatrix, 0);
            Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.setIdentityM(mMvpMatrix, 0);
            Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);

            GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mMvpMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, faceCubeBuffer.capacity() / 3);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        }

        rotateY += 3;

        onDrawArraysAfter();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setFaceInfo(final List<float[]> points, final List<float[]> points2d, final List<RectF> translateInImages, final List<Float> scales, final int width, final int height, final PointF cubeDistance, final PointF textureDistance, final boolean isTranspose) {
        if (!isShow) {
            return;
        }
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mPoints = points;
                mPoints2d = points2d;
                mTranslateInImages = translateInImages;
                mScales = scales;
                mWidth = width;
                mHeight = height;
                mIsTranspose = isTranspose;
                mTextureDistance = textureDistance;
                mCubeDistance = cubeDistance;
                updateFaceCubeBuffer(cubeDistance, textureDistance, isTranspose);
            }
        });
    }

    private void updateFaceCubeBuffer(PointF cubeDistance, PointF textureDistance, boolean transpose) {
        faceCubeBuffers.clear();
        int width = mWidth;
        int height = mHeight;
        RectF textureCoords = new RectF(textureDistance.x, textureDistance.y, 1f - textureDistance.x, 1f - textureDistance.y);
        if(transpose) {
            width = mHeight;
            height = mWidth;
            textureCoords = new RectF(textureDistance.y, textureDistance.x, 1f - textureDistance.y, 1f - textureDistance.x);
        }

        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.orthoM(mProjectionMatrix, 0, width/2*textureCoords.width(), -width/2*textureCoords.width(),
                height/2*textureCoords.height(), -height/2*textureCoords.height(), -1000, 1000);

        if (mPoints != null) {
            int count = mPoints.size();
            for(int k = 0; k < count; k++) {
                float[] point = mPoints.get(k);
                float scale = mScales.get(k) / 2;
                final int pointCount = point.length / 3;
                FloatBuffer faceCubeBuffer = OpenGlUtils.createFloatBuffer(point.length);
                for (int i = 0; i < pointCount; i++) {
                    float x = point[3 * i] * scale;
                    float y = point[3 * i + 1] * scale;
                    float z = point[3 * i + 2] * scale;

//                    x = (width/2) - x;
//                    y = (height/2) - y;

                    faceCubeBuffer.put(x);
                    faceCubeBuffer.put(y);
                    faceCubeBuffer.put(z);
                }
                // test
//                float x = point[3 * 30] * scale;
//                float y = point[3 * 30 + 1] * scale;
//                float z = (point[3 * 30 + 2] - 10f) * scale;
//                faceCubeBuffer.put(x);
//                faceCubeBuffer.put(y);
//                faceCubeBuffer.put(z);
                faceCubeBuffer.position(0);
                faceCubeBuffers.add(faceCubeBuffer);
            }
        }
    }

    public void show(boolean isShow) {
        this.isShow = isShow;
    }
}
