package com.library.aimo.util;

import android.util.Log;

import com.library.aimo.BuildConfig;


/**
 * Created by joybar on 2017/5/17.
 */

public class ImoLog {

    private static int LEVEL = Log.DEBUG;//默认level
    private static String TAG = "AiMall-Log";//默认tag
    private static boolean isOpenFileLog = false;

    static {
    }

    public static void v(Object msg) {
        performPrint(Log.VERBOSE, TAG, msg.toString());
    }

    public static void d(Object msg) {
        performPrint(Log.DEBUG, TAG, msg.toString());
    }

    public static void i(Object msg) {
        performPrint(Log.INFO, TAG, msg.toString());
    }

    public static void w(Object msg) {
        performPrint(Log.WARN, TAG, msg.toString());
    }

    public static void e(Object msg) {
        performPrint(Log.ERROR, TAG, msg.toString());
    }

    public static void a(Object msg) {
        performPrint(Log.ASSERT, TAG, msg.toString());
    }

    public static void v(String tag, Object msg) {
        performPrint(Log.VERBOSE, tag, msg.toString());
    }

    public static void d(String tag, Object msg) {
        performPrint(Log.DEBUG, tag, msg.toString());
    }

    public static void i(String tag, Object msg) {
        performPrint(Log.INFO, tag, msg.toString());
    }

    public static void w(String tag, Object msg) {
        performPrint(Log.WARN, tag, msg.toString());
    }

    public static void e(String tag, Object msg) {
        performPrint(Log.ERROR, tag, msg.toString());
    }

    public static void a(String tag, Object msg) {
        performPrint(Log.ASSERT, tag, msg.toString());
    }

    //设置默认的Level
    public static void setDefaultLevel(int level) {
        ImoLog.LEVEL = level;
    }

    //设置默认的TAG
    public static void setDefaultTag(String tag) {
        ImoLog.TAG = tag;
    }

    //打印
    public static void print(String msg) {
        performPrint(LEVEL, TAG, msg);
    }

    //打印-自定义Tag
    public static void print(String tag, String msg) {
        performPrint(LEVEL, tag, msg);
    }

    //打印-自定义Level
    public static void print(int level, String msg) {
        performPrint(level, TAG, msg);
    }

    //打印-自定义Tag,自定义Level
    public static void print(int level, String tag, String msg) {
        performPrint(level, tag, msg);
    }

    //执行打印
    private static void performPrint(int level, String tag, String msg) {

        //Release版本不打印log，Log.WARN及以上级别log除外
        if (BuildConfig.DEBUG) {
            String threadName = Thread.currentThread().getName();
            String lineIndicator = getLineIndicator();
            Log.println(level, tag, threadName + " " + lineIndicator + " " + msg);
        }
        // Log.VERBOSE级别的log不做文件存储
        if (isOpenFileLog && level > Log.VERBOSE) {
//            FileLogger.print2File(level, tag, msg);
        }
    }

    //获取行所在的方法指示
    //获取行所在的方法指示
    private static String getLineIndicator() {
        //3代表方法的调用深度：0-getLineIndicator，1-performPrint，2-print，3-调用该工具类的方法位置
        StackTraceElement element = ((new Exception()).getStackTrace())[3];
        String sb = "(" +
                element.getFileName() + ":" +
                element.getLineNumber() + ")." +
                element.getMethodName() + ":";
        return sb;
    }

    public static void openFileLog(boolean isOpen) {
        openFileLog(isOpen, true);
    }

    public static void openFileLog(boolean isOpen, boolean fileFlushSwitch) {
        d("L.openFileLog 0 isOpen=" + isOpen + " fileFlushSwitch:" + fileFlushSwitch);
        isOpenFileLog = isOpen;
//        FileLogger.getConfig().setFlushLogSwitch(fileFlushSwitch);
//        if (fileFlushSwitch) {
//            FileLogger.clearLog(7);
//        }
        d("L.openFileLog 1 isOpen=" + isOpen + " fileFlushSwitch:" + fileFlushSwitch);
    }
//
//    public static void onDestory() {
//        FileLogger.stopLogThread();
//    }
//
//    public static void syncTime(long timeDis) {
//        FileLogger.syncTime(timeDis);
//    }
//
//    public static void forceFlashLog() {
//        FileLogger.forceFlashLog();
//    }
}
