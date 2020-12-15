package com.library.aimo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

/**
 * ================================================
 * 作    者：aguai（吴红斌）Github地址：https://github.com/aguai1
 * 版    本：1.0
 * 创建日期：17-12-8
 * 描    述：
 * ================================================
 */
public class EasyLibUtils {
    public static WeakReference<Activity> sTopActivityWeakRef;
    private static Application sApplication;
    private static Application.ActivityLifecycleCallbacks mCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            setTopActivityWeakRef(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
            setTopActivityWeakRef(activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            setTopActivityWeakRef(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    };

    /**
     * 初始化工具类
     *
     * @param app 应用
     */
    public static void init(@NonNull final Application app) {
        EasyLibUtils.sApplication = app;
        app.registerActivityLifecycleCallbacks(mCallbacks);
    }


    /**
     * 获取 Application
     *
     * @return Application
     */
    public static Application getApp() {
        if (sApplication != null) return sApplication;
        throw new NullPointerException("u should init first");
    }

    private static void setTopActivityWeakRef(Activity activity) {
        if (sTopActivityWeakRef == null || !activity.equals(sTopActivityWeakRef.get())) {
            sTopActivityWeakRef = new WeakReference<>(activity);
        }
    }
}
