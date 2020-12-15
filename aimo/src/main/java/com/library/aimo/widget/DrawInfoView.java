package com.library.aimo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

import com.library.aimo.util.DrawInterface;
import com.library.aimo.util.ImoLog;

/**
 * Created by whb
 * 外部绘制控件
 */
public class DrawInfoView extends View {
    private DrawInterface drawInterface;
    private Matrix matrix = new Matrix();
    private float scale = 1.0f;
    private int mInputWidth = 0;
    private int mInputHeight = 0;
    private int mMeasureWidth = 0;
    private int mMeasureHeight = 0;
    private boolean isCenterCrop = true;

    public DrawInfoView(Context context) {
        super(context);
        init();
    }

    public DrawInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawColor(0, PorterDuff.Mode.CLEAR); // android things中会绘制黑色，这里注释,没有必要加,SurfaceView中也许需要
        try {
            if (drawInterface != null) {
                drawInterface.onDrawToScreen(canvas);
                canvas.concat(matrix);
                drawInterface.onDrawToPic(canvas, scale);
            }
        } catch (Exception e) {
            ImoLog.e("EasyLibs:" + e.toString());
        }
    }
    public void setDrawInterface(DrawInterface drawInterface) {
        this.drawInterface = drawInterface;
    }

    public void postInvalidate(int width, int height) {
        if(getMeasuredWidth() == 0) {
            return;
        }
        if (mInputWidth != width || mInputHeight != height || mMeasureWidth != getMeasuredWidth() || mMeasureHeight != getMeasuredHeight()) {
            mMeasureWidth = getMeasuredWidth();
            mMeasureHeight = getMeasuredHeight();
            mInputWidth = width;
            mInputHeight = height;
            matrix = new Matrix();
            float sh = mMeasureHeight * 1f / height;
            float sw = mMeasureWidth * 1f / width;
            scale = isCenterCrop ? Math.max(sw, sh) : Math.min(sw, sh);
            matrix.postTranslate(mMeasureWidth / 2f - width / 2f, mMeasureHeight / 2f - height / 2f);
            matrix.postScale(scale, scale, mMeasureWidth / 2f, mMeasureHeight / 2f);
        }
        postInvalidate();
    }

    public void setCenterCrop(boolean centerCrop) {
        isCenterCrop = centerCrop;
    }
}
