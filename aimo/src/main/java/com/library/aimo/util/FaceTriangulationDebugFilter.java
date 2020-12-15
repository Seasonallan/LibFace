package com.library.aimo.util;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.library.aimo.R;
import com.library.aimo.gpuimage.GPUImageFilter;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by zhangchao on 17-10-20.
 */

public class FaceTriangulationDebugFilter extends GPUImageFilter {

    private boolean isShow;
    private float[] mPoints;
    private int width;
    private int height;

    private int radiusUniform;
    private int pointColorUniform;

    private FloatBuffer faceCubeBuffer;
    private ShortBuffer indices;

    private short[] faceTriangulationLines;

    public FaceTriangulationDebugFilter() {
        super(OpenGlUtils.readShaderFromRawResource(R.raw.lines_vertex),
                OpenGlUtils.readShaderFromRawResource(R.raw.lines_fragment));
        setIsBlend(true);

        faceTriangulationLines = triangles2lines(ConstantTriangulation.getFaceTriangles());

        indices = OpenGlUtils.createShortBuffer(faceTriangulationLines);
        setIsBlend(true);
    }

    private short[] triangles2lines(short[] faceTriangles) {
        short[] lines = new short[faceTriangles.length * 2];

        for(int i = 0; i < faceTriangles.length; i += 3) {
            int linesIndex = i * 2;
            lines[linesIndex] = faceTriangles[i];
            lines[linesIndex + 1] = faceTriangles[i + 1];
            lines[linesIndex + 2] = faceTriangles[i];
            lines[linesIndex + 3] = faceTriangles[i + 2];
            lines[linesIndex + 4] = faceTriangles[i + 1];
            lines[linesIndex + 5] = faceTriangles[i + 2];
        }
        return lines;
    }

    @Override
    public void onInit() {
        super.onInit();
        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        pointColorUniform = GLES20.glGetUniformLocation(getProgram(), "pointColor");
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        if(!isShow) {
            return;
        }
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        if(mPoints == null) {
            return;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mTargetFrameBuffer);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        if (!isBlend()) {
            clearColor();
        }
        faceCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, faceCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glUniform1f(radiusUniform, 10);
        GLES20.glUniform4f(pointColorUniform, 0, 1, 0, 1);
        onDrawArraysPre();
        GLES20.glDrawElements(GLES20.GL_LINES, faceTriangulationLines.length, GLES20.GL_UNSIGNED_SHORT, indices);
        onDrawArraysAfter();
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setFaceInfo(float[] points, int width, int height, final PointF textureDistance) {
        mPoints = points;
        this.width = width;
        this.height = height;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                updateFaceCubeBuffer(textureDistance);
            }
        });
    }

    private void updateFaceCubeBuffer(PointF textureDistance) {
        if (mPoints== null){
            faceCubeBuffer = OpenGlUtils.createFloatBuffer(0);
        }else {
            RectF textureCoords = new RectF(textureDistance.y, textureDistance.x, 1f - textureDistance.y, 1f - textureDistance.x);

            final int pointCount = mPoints.length / 2;
            faceCubeBuffer = OpenGlUtils.createFloatBuffer(mPoints.length);
            for(int i = 0; i < pointCount; i++) {
                float x = mPoints[2 * i];
                float y = mPoints[2 * i + 1];

                float xT = x / height;
                float yT = y / width;

                float cubeX = (2f * xT - 1f) / textureCoords.width();
                float cubeY = (1f - 2f * yT) / textureCoords.height();
                faceCubeBuffer.put(cubeX);
                faceCubeBuffer.put(cubeY);
            }
        }
        faceCubeBuffer.position(0);
    }


    public void show(boolean isShow) {
        this.isShow = isShow;
    }
}
