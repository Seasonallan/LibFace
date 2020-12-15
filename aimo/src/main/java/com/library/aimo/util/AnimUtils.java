package com.library.aimo.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

import static android.animation.ValueAnimator.RESTART;
import static android.view.animation.Animation.INFINITE;


/**
 * Created by whb on 17-9-28.
 */

public class AnimUtils {
    public static final int COMPARE_ANIM_TIME = 600;
    private static final String TAG = "AnimUtils";

    public static void hideView(View view, int time) {
        if (view == null) {
            return;
        }
        ImoLog.d(TAG, "AnimUtil.hideView time=" + time);
        if (Float.compare(view.getAlpha(), 0) != 0 && time > 0) {
            view.clearAnimation();
            ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            anim.setDuration(time);
            anim.start();
        }
        view.setAlpha(0f);
    }

    public static void hideView(View view) {
        hideView(view, COMPARE_ANIM_TIME);
    }

    public static void showView(View view) {
        if (view == null) {
            return;
        }
        ImoLog.d(TAG, "AnimUtil.showView");
        if (Float.compare(view.getAlpha(), 1) != 0) {
            view.clearAnimation();
            ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            anim.setDuration(COMPARE_ANIM_TIME);
            anim.start();
        }
        view.setAlpha(1f);
    }

//    public static void translateView(View moveView, View desView, long time, AnimatorListenerAdapter animListener) {
//        if (moveView == null) {
//            return;
//        }
//        int[] locSrc = new int[2];
//        int[] locDst = new int[2];
//        moveView.getLocationOnScreen(locSrc);
//        desView.getLocationOnScreen(locDst);
//        int moveX = locDst[0] - locSrc[0];
//        int moveY = locDst[1] - locSrc[1];
//        translateView(moveView, new PointF(moveX, moveY), time, 0, animListener, false);
//    }
//
//    public static void translateView(View moveView, View srcViewPos,View desView, long time, AnimatorListenerAdapter animListener) {
//        if (moveView == null) {
//            return;
//        }
//        int[] locSrc = new int[2];
//        int[] locDst = new int[2];
//        srcViewPos.getLocationOnScreen(locSrc);
//        desView.getLocationOnScreen(locDst);
//        int moveX = locDst[0] - locSrc[0];
//        int moveY = locDst[1] - locSrc[1];
//
//        int startX = 0;
//        int startY = 0;
//        translateView(moveView, new PointF(startX,startY),new PointF(moveX, moveY), time, 0, animListener,false,null);
//    }


    public static Animator translateView(View moveView, View desView, long time, AnimatorListenerAdapter animListener) {
        return translateView(moveView, moveView, desView, time, animListener);
    }

    public static Animator translateView(View moveView, View srcViewPos, View desView, long time, AnimatorListenerAdapter animListener) {
        if (moveView == null) {
            return null;
        }

        int[] locMove = new int[2];
        int[] locSrc = new int[2];
        int[] locDst = new int[2];
        moveView.getLocationOnScreen(locMove);
        srcViewPos.getLocationOnScreen(locSrc);
        desView.getLocationOnScreen(locDst);
        int fromX = locSrc[0] - locMove[0];
        int fromY = locSrc[1] - locMove[1];
        int toX = locDst[0] - locMove[0];
        int toY = locDst[1] - locMove[1];

        return translateView(moveView, new PointF(fromX,fromY),new PointF(toX, toY), time, 0, animListener,false,null);
    }

    public static Animator translateViewX(View moveView, float precent, long time, long startDelay, AnimatorListenerAdapter animListener, boolean repate) {
        int moveX = (int) (moveView.getWidth() * precent);
        int moveY = 0;
        return translateView(moveView, new PointF(moveX, moveY), time, startDelay, animListener, repate);
    }

    public static Animator translateViewY(View moveView, float precent, long time, long startDelay, AnimatorListenerAdapter animListener, boolean repate) {
        return translateViewY(moveView, precent, time, startDelay, animListener, repate, null);
    }

    public static Animator translateViewX(View moveView, float precent, long time, long startDelay, AnimatorListenerAdapter animListener, boolean repate, boolean reverse) {
        int moveX = (int) (moveView.getWidth() * precent);
        int moveY = 0;
        return translateView(moveView, new PointF(0, 0), new PointF(moveX, moveY), time, startDelay, animListener, repate, reverse, null);
    }

    public static Animator translateViewY(View moveView, float precent, long time, long startDelay, AnimatorListenerAdapter animListener, boolean repate, TimeInterpolator timeInterpolator) {
        int moveX = 0;
        int moveY = (int) (moveView.getHeight() * precent);
        return translateView(moveView, new PointF(moveX, moveY), time, startDelay, animListener, repate, timeInterpolator);
    }

    public static Animator translateView(View moveView, PointF move, long time, long startDelay, AnimatorListenerAdapter animListener, boolean lopperAndRepate) {
        return translateView(moveView, move, time, startDelay, animListener, lopperAndRepate, null);
    }

    public static Animator translateView(View moveView, PointF move, long time, long startDelay, AnimatorListenerAdapter animListener, boolean lopperAndRepate, TimeInterpolator timeInterpolator) {
        return translateView(moveView, new PointF(0, 0), new PointF(move.x, move.y), time, startDelay, animListener, lopperAndRepate, timeInterpolator);
    }

    public static Animator translateView(View moveView, PointF from, PointF to, long time, long startDelay, AnimatorListenerAdapter animListener, boolean lopperAndRepate, TimeInterpolator timeInterpolator) {
        return translateView(moveView, from, to, time, startDelay, animListener, lopperAndRepate, false, timeInterpolator);
    }

    public static Animator translateView(View moveView, PointF from, PointF to, long time, long startDelay, AnimatorListenerAdapter animListener, boolean lopperAndRepate, boolean reverse, TimeInterpolator timeInterpolator) {
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(moveView, "translationX", from.x, to.x);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(moveView, "translationY", from.y, to.y);
        if (null != animListener) {
            animator1.addListener(animListener);
        }
        if (lopperAndRepate) {
            animator1.setRepeatCount(INFINITE);
            animator2.setRepeatCount(INFINITE);
        }
        if (reverse) {
            animator1.setRepeatMode(ValueAnimator.REVERSE);
            animator2.setRepeatMode(ValueAnimator.REVERSE);
        }
        List<Animator> animators = new ArrayList<>();
        animators.add(animator1);
        animators.add(animator2);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.setDuration(time);
        animatorSet.setStartDelay(startDelay);
        if (null != timeInterpolator) {
            animatorSet.setInterpolator(timeInterpolator);
        }
        animatorSet.start();

        return animatorSet;
    }

    public static Animator scaleView(View srcView, View destView, long time, long startDelay, AnimatorListenerAdapter animListener) {
        return scaleView(srcView, time, startDelay, animListener, srcView.getScaleX(), destView.getScaleX());
    }

    public static Animator scaleView(View scaleView, View srcView, View destView, long time, long startDelay, AnimatorListenerAdapter animListener) {
        return scaleView(scaleView, time, startDelay, animListener, srcView.getScaleX(), destView.getScaleX());
    }

    public static Animator scaleView(View view, long duration, long startDelay, AnimatorListenerAdapter animListener, float... scales) {
        return scaleView(view, duration, startDelay, animListener, null, scales);
    }

    public static Animator scaleView(View view, long duration, long startDelay, AnimatorListenerAdapter animListener, TimeInterpolator timeInterpolator, float... scales) {
        return scaleView(view, duration, startDelay, animListener, timeInterpolator, 0, RESTART, scales);
    }

    public static Animator scaleView(View view, long duration, long startDelay, AnimatorListenerAdapter animListener, TimeInterpolator timeInterpolator, int repeatCount, int repeatMode, float... scales) {
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(view, "scaleX", scales);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "scaleY", scales);
        if (null != animListener) {
            animator1.addListener(animListener);
        }
        if (repeatCount != 0) {
            animator1.setRepeatCount(repeatCount);
            animator2.setRepeatCount(repeatCount);
            animator1.setRepeatMode(repeatMode);
            animator2.setRepeatMode(repeatMode);
        }

        List<Animator> animators = new ArrayList<>();
        animators.add(animator1);
        animators.add(animator2);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.setDuration(duration);
        animatorSet.setStartDelay(startDelay);
        if (null != timeInterpolator) {
            animatorSet.setInterpolator(timeInterpolator);
        }
        animatorSet.start();
        return animatorSet;
    }

    public static Animator alphaView(View view, float fromValue, float toValue, long duration) {
        return alphaView(view, fromValue, toValue, duration, null);
    }

    public static Animator alphaView(View view, float fromValue, float toValue, long duration, AnimatorListenerAdapter animatorListener) {
        return alphaView(view, fromValue, toValue, duration, 0, animatorListener);
    }

    public static Animator alphaView(View view, float fromValue, float toValue, long duration, long startDelay, AnimatorListenerAdapter animatorListener) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "Alpha", fromValue, toValue);
        animator.setDuration(duration);
        animator.setStartDelay(startDelay);
        if (null != animatorListener) {
            animator.addListener(animatorListener);
        }
        animator.start();
        return animator;
    }

    public static void unSelView(final View view, long time) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setSelected(false);
            }
        }, time);
    }

    public static void startScaleEnterAnim(View animImageView, Rect srcBitmapRect, Rect dstBitmapRect, Interpolator decelerateInterpolator, AnimatorListenerAdapter animatorListenerAdapter, int time) {

        float sw = 1.0f * srcBitmapRect.width() / dstBitmapRect.width();
        float sh = 1.0f * srcBitmapRect.height() / dstBitmapRect.height();

        animImageView.setScaleX(sw);
        animImageView.setScaleY(sh);

        animImageView.setTranslationX(srcBitmapRect.left - dstBitmapRect.left + (srcBitmapRect.width() - dstBitmapRect.width()) / 2);
        animImageView.setTranslationY(srcBitmapRect.top - dstBitmapRect.top + (srcBitmapRect.height() - dstBitmapRect.height()) / 2);

        ViewPropertyAnimator viewPropertyAnimator = animImageView.animate()
                .scaleX(1)
                .scaleY(1)
                .translationX(0)
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator());
        if (time != 0) {
            viewPropertyAnimator.setDuration(time);
        }
        if (decelerateInterpolator != null) {
            viewPropertyAnimator.setInterpolator(decelerateInterpolator);
        }
        if (animatorListenerAdapter != null) {
            viewPropertyAnimator.setListener(animatorListenerAdapter);
        }
        viewPropertyAnimator.start();
    }

    public static void startScaleExitAnim(View animImageView, Rect srcBitmapRect, Rect dstBitmapRect, Interpolator decelerateInterpolator, AnimatorListenerAdapter animatorListenerAdapter, int time) {

        float sw = 1.0f * srcBitmapRect.width() / dstBitmapRect.width();
        float sh = 1.0f * srcBitmapRect.height() / dstBitmapRect.height();

        ViewPropertyAnimator viewPropertyAnimator = animImageView.animate()
                .scaleX(sw)
                .scaleY(sh)
                .translationX(srcBitmapRect.left - dstBitmapRect.left + (srcBitmapRect.width() - dstBitmapRect.width()) / 2)
                .translationY(srcBitmapRect.top - dstBitmapRect.top + (srcBitmapRect.height() - dstBitmapRect.height()) / 2)
                .setInterpolator(new DecelerateInterpolator());
        if (time != 0) {
            viewPropertyAnimator.setDuration(time);
        }
        if (decelerateInterpolator != null) {
            viewPropertyAnimator.setInterpolator(decelerateInterpolator);
        }
        if (animatorListenerAdapter != null) {
            viewPropertyAnimator.setListener(animatorListenerAdapter);
        }
        viewPropertyAnimator.start();
    }


    public static void popupAnim(View view) {
        long duration = 1000;
        TimeInterpolator interpolator = new BounceInterpolator();
        float dstScaleX = view.getScaleX();
        float dstScaleY = view.getScaleY();

        ObjectAnimator animatorX = ObjectAnimator.ofFloat(view, "scaleX", 0, dstScaleX);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(view, "scaleY", 0, dstScaleY);
        List<Animator> animators = new ArrayList<>();
        animators.add(animatorX);
        animators.add(animatorY);
        AnimatorSet btnSexAnimatorSet = new AnimatorSet();
        btnSexAnimatorSet.playTogether(animators);
        btnSexAnimatorSet.setDuration(duration);
        btnSexAnimatorSet.setInterpolator(interpolator);
//        btnSexAnimatorSet.setInterpolator(new OvershootInterpolator());
        btnSexAnimatorSet.start();
    }

    public static Animator rotationAnim(View view, float fromDegrees, float toDegrees, long duration, long startDelay, int repeatCount, int repeatMode) {
        return rotationAnim(view, fromDegrees, toDegrees, duration, startDelay, repeatCount, repeatMode, null);
    }

    public static Animator rotationAnim(View view, float fromDegrees, float toDegrees, long duration, long startDelay, int repeatCount, int repeatMode, TimeInterpolator timeInterpolator) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "Rotation", fromDegrees, toDegrees);
        animator.setDuration(duration);
        animator.setRepeatCount(repeatCount);//无限循环
        animator.setRepeatMode(repeatMode);
        animator.setStartDelay(startDelay);
        if (null != timeInterpolator) {
            animator.setInterpolator(timeInterpolator);
        }
        animator.start();
        return animator;
    }
    public interface AnimEndListener {
        void onAnimEnd();
    }
}
