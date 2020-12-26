package com.library.aimo.widget;

import android.animation.ValueAnimator;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

import com.library.aimo.R;
import com.library.aimo.api.IFaceAction;


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


        circlePaint = new Paint();
        circlePaint.setAntiAlias(true); // 抗锯齿
        circlePaint.setDither(true); // 防抖动
        circlePaint.setStrokeWidth(5 * getContext().getResources().getDisplayMetrics().density);

    }

    Paint circlePaint;


    private View loadingView;
    private Animation anim;

    public static final int MODE_ROTATE = -1;
    public static final int MODE_PROGRESS = 1;
    private int mode = MODE_ROTATE;
    /**
     * 设置进度模式
     */
    public void setModeProgress(){
        setMode(MODE_PROGRESS);
    }
    public void setMode(int mode){
        this.mode = mode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        loadingView = findViewById(R.id.face_view_loading);
        rotateAnim();
    }

    public void rotateAnim() {
        if (mode == MODE_ROTATE){
            loadingView.setBackgroundResource(R.drawable.icon_progress);
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
        }else{

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
        if (loadingView == null) {
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

        if (mode == MODE_PROGRESS){
            circlePaint.setShader(null); // 清除上一次的shader
            circlePaint.setColor(0xffd3d3d3); // 设置底部圆环的颜色，这里使用第一种颜色
            circlePaint.setStyle(Paint.Style.STROKE); // 设置绘制的圆为空心

            int circleWidth = (int) circlePaint.getStrokeWidth();
            int center = this.getWidth() / 2;
            int radius = (int) (center - circleWidth / 2);

            canvas.drawCircle(center, center, radius, circlePaint); // 画底部的空心圆
            RectF oval = new RectF(center - radius, center - radius, center + radius, center + radius); // 圆的外接正方形

            // 绘制颜色圆环
            circlePaint.setColor(0xff0178ff); // 设置圆弧的颜色
            circlePaint.setStrokeCap(Paint.Cap.ROUND); // 把每段圆弧改成圆角的

            canvas.drawArc(oval, 90, currentValue * 360.0f / 100 * 1.0f, false, circlePaint);

        }
    }

    public void showSuccess() {
        if (loadingView != null)
            loadingView.clearAnimation();
    }

    /**
     * 当前进度值
     */
    private int currentValue = 0;

    ValueAnimator animator;
    /**
     * 按进度显示百分比，可选择是否启用数字动画
     *
     * @param percent      进度，值通常为0到100
     * @param useAnimation 是否启用动画，true为启用
     */
    public void setProgress(int percent, boolean useAnimation) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        if (useAnimation) // 使用动画
        {
            if (animator != null){
                animator.cancel();
                animator.removeAllListeners();
            }
            animator = ValueAnimator.ofInt(currentValue, percent);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    currentValue = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.setInterpolator(new AccelerateInterpolator());
            animator.setDuration(600);
            animator.start();
        } else {
            setProgress(percent);
        }
    }


    /**
     * 按进度显示百分比
     *
     * @param percent 进度，值通常为0到100
     */
    public void setProgress(int percent) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        this.currentValue = percent;
        invalidate();
    }


    public void restart() {
        anim = null;
        rotateAnim();
    }
}
