package com.library.aimo.util;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;

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
public class FaceRendererRotation extends GPUImageFilter {

    private boolean isShow = true;
    private List<float[]> mPoints;
    private List<float[]> mRotations;
    private List<Float> mScales;
    private int mWidth;
    private int mHeight;
    private boolean mIsTranspose;
    private PointF mTextureDistance;
    private PointF mCubeDistance;
    private List<FloatBuffer> faceCubeBuffers = new ArrayList<>();
    private ShortBuffer rotationIndicesBuffer;
    private static final short[] rotationIndices = new short[]{0, 1
            , 2, 3, 2, 4, 5, 3, 5, 4
            , 6, 7, 6, 8, 9, 7, 9, 8
            , 2, 6, 3, 7, 4, 8, 5, 9
    };
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

    public FaceRendererRotation() {
        super(OpenGlUtils.readShaderFromRawResource(R.raw.pts3d_vertex),
                OpenGlUtils.readShaderFromRawResource(R.raw.lines_fragment));
        setIsBlend(true);
        lineWidth = 5;
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -5, 0, 0, 0, 0, 1, 0);
        rotationIndicesBuffer = OpenGlUtils.createShortBuffer(rotationIndices);
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
        GLES20.glUniform4f(pointColorUniform, 0, 0, 1, 1);
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

//            float[] point2d = mPoints2d.get(i);
//            float[] translateInImage = new float[]{point2d[27 * 2] - (point2d[33 * 2] - point2d[27 * 2]), point2d[27 * 2 + 1] - (point2d[33 * 2 + 1] - point2d[27 * 2 + 1])};
//            RectF rectF = mTranslateInImages.get(i);
//            float[] translateInImage = {rectF.centerX(), rectF.centerY()};
            Matrix.setIdentityM(mModelMatrix, 0);
//            Matrix.translateM(mModelMatrix, 0, translateInImage[0] - width / 2, translateInImage[1] - height * 3 / 4, 0);
            Matrix.rotateM(mModelMatrix, 0, rotateY, 0, 1, 0);

            Matrix.setIdentityM(mModelViewMatrix, 0);
            Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.setIdentityM(mMvpMatrix, 0);
            Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);

            GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mMvpMatrix, 0);

//            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, faceCubeBuffer.capacity() / 3);
            GLES20.glDrawElements(GLES20.GL_LINES, rotationIndices.length, GLES20.GL_UNSIGNED_SHORT, rotationIndicesBuffer);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        }

//        rotateY += 3;

        onDrawArraysAfter();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setFaceInfo(final List<float[]> points, final List<float[]> rotations, final List<Float> scales, final int width, final int height, final PointF cubeDistance, final PointF textureDistance, final boolean isTranspose) {
        if (!isShow) {
            return;
        }
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mPoints = points;
                mRotations = rotations;
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

        if (mPoints != null && mRotations != null) {
            int count = mPoints.size();
            for(int k = 0; k < count; k++) {
                float[] point = mPoints.get(k);
                float[] rotation = mRotations.get(k);
                float scale = mScales.get(k);
                FloatBuffer faceCubeBuffer = OpenGlUtils.createFloatBuffer(6 + 8 * 3);

                float[] center = new float[3];
                center[0] = point[2 * 30] - width / 2;//(point[2 * 30] / width * 2 - 1);
                center[1] = point[2 * 30 + 1] - height / 2;//(point[2 * 30 + 1] / height * 2 - 1);
                center[2] = 0;
                faceCubeBuffer.put(center);
                float radius = 25 * scale;
                float[] resultVec = getRotationPoint(rotation, new float[] {0, 0, radius});
                faceCubeBuffer.put(offsetPoint(center, resultVec));

                float cubeRadius = 28 * scale;
                float[] flt = getRotationPoint(rotation, new float[] {-cubeRadius, cubeRadius, cubeRadius});
                float[] frt = getRotationPoint(rotation, new float[] {cubeRadius, cubeRadius, cubeRadius});
                float[] flb = getRotationPoint(rotation, new float[] {-cubeRadius, -cubeRadius, cubeRadius});
                float[] frb = getRotationPoint(rotation, new float[] {cubeRadius, -cubeRadius, cubeRadius});
                float[] blt = getRotationPoint(rotation, new float[] {-cubeRadius, cubeRadius, -cubeRadius});
                float[] brt = getRotationPoint(rotation, new float[] {cubeRadius, cubeRadius, -cubeRadius});
                float[] blb = getRotationPoint(rotation, new float[] {-cubeRadius, -cubeRadius, -cubeRadius});
                float[] brb = getRotationPoint(rotation, new float[] {cubeRadius, -cubeRadius, -cubeRadius});


                center = new float[] {point[2 * 28] - width / 2, point[2 * 28 + 1] - height / 2, 0};
                faceCubeBuffer.put(offsetPoint(center, flt));
                faceCubeBuffer.put(offsetPoint(center, frt));
                faceCubeBuffer.put(offsetPoint(center, flb));
                faceCubeBuffer.put(offsetPoint(center, frb));
                faceCubeBuffer.put(offsetPoint(center, blt));
                faceCubeBuffer.put(offsetPoint(center, brt));
                faceCubeBuffer.put(offsetPoint(center, blb));
                faceCubeBuffer.put(offsetPoint(center, brb));


                faceCubeBuffer.position(0);
                faceCubeBuffers.add(faceCubeBuffer);
            }
        }
    }

    public float[] getRotationPoint(float[] rotation, float[] point) {
        float[]  matrixX = new float[] {
                1,       0,              0, 0,
                0,       (float) Math.cos(rotation[0]),   (float)-Math.sin(rotation[0]), 0,
                0,       (float) Math.sin(rotation[0]),   (float) Math.cos(rotation[0]),  0,
                0,       0,              0, 1,
        };

        float[]  matrixY = new float[] {
                (float) Math.cos(rotation[1]),    0,      (float) Math.sin(rotation[1]), 0,
                0,               1,      0, 0,
                (float)-Math.sin(rotation[1]),   0,      (float) Math.cos(rotation[1]), 0,
                0,       0,              0, 1,
        };

        float[]  matrixZ = new float[] {
                (float) Math.cos(rotation[2]),      (float)-Math.sin(rotation[2]),     0, 0,
                (float) Math.sin(rotation[2]),      (float) Math.cos(rotation[2]),      0, 0,
                0,                  0,                  1, 0,
                0,       0,              0, 1,
        };
        float[] matrixXYZ = new float[16];
        Matrix.setIdentityM(matrixXYZ, 0);
        Matrix.multiplyMM(matrixXYZ, 0, matrixX, 0, matrixY, 0);
        Matrix.multiplyMM(matrixXYZ, 0, matrixXYZ, 0, matrixZ, 0);
        float[] resultVec = new float[4];
        Matrix.multiplyMV(resultVec, 0, matrixXYZ, 0, new float[]{ point[0], point[1], point[2], 0}, 0);
        return new float[] { resultVec[0], resultVec[1], resultVec[2] };
    }

    private float[] offsetPoint(float[] origin, float[] offset) {
        return new float[] { origin[0] + offset[0], origin[1] + offset[1], origin[2] + offset[2] };
    }

    public void show(boolean isShow) {
        this.isShow = isShow;
    }
}
