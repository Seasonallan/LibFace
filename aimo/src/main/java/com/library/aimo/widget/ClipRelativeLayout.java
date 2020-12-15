package com.library.aimo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

import com.library.aimo.R;


public class ClipRelativeLayout extends RelativeLayout {
    private Paint clipPaint;


    public ClipRelativeLayout(Context context) {
        super(context);
        init();
    }

    public ClipRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClipRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        loadingView = findViewById(R.id.face_view_loading);

        Xfermode xFermode = new PorterDuffXfermode(PorterDuff.Mode.DARKEN);
        clipPaint = new Paint();
        clipPaint.setStyle(Paint.Style.FILL);
        clipPaint.setColor(Color.parseColor("#0181FF"));
        clipPaint.setStrokeWidth(.1f);
        clipPaint.setAntiAlias(true);
        clipPaint.setXfermode(xFermode);
    }


    private View loadingView;

    private Animation anim;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        loadingView = findViewById(R.id.face_view_loading);

        rotateAnim();
    }

    public void rotateAnim() {
        if (null == anim) {
            anim = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setFillAfter(true); // 设置保持动画最后的状态
            anim.setDuration(1300); // 设置动画时间
            anim.setInterpolator(new LinearInterpolator()); // 设置插入器
            anim.setRepeatCount(-1);
            anim.setRepeatMode(Animation.RESTART);
            loadingView.clearAnimation();
            loadingView.startAnimation(anim);
        }
    }

    private Path circlePath = new Path();

    private void initCirclePath() {
        int x = (loadingView.getLeft() + loadingView.getRight()) / 2;
        int y = (loadingView.getTop() + loadingView.getBottom()) / 2;
        Path path = circlePath;
        path.reset();
        //设置裁剪的圆心，半径
        path.addCircle(x, y, loadingView.getWidth() / 2 - 4 * getResources().getDisplayMetrics().density, Path.Direction.CCW);
    }

    public RectF getArea() {
        if (loadingView == null){
            return null;
        }
        return new RectF(loadingView.getLeft(), loadingView.getTop(),
                loadingView.getWidth() + loadingView.getLeft(), loadingView.getTop() + loadingView.getHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }


    @Override
    public void draw(Canvas canvas) {
        initCirclePath();
        Path path = circlePath;

        //裁剪画布，并设置其填充方式
        canvas.save();
        canvas.clipPath(path, Region.Op.DIFFERENCE);
        canvas.drawPath(path, clipPaint);

        super.draw(canvas);//绘制其他控件
        canvas.restore();
    }

    public void showSuccess() {
        if (loadingView != null)
            loadingView.clearAnimation();
    }

    public void restart() {
        anim = null;
        rotateAnim();
    }
}
