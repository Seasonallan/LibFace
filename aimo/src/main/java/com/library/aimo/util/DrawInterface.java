package com.library.aimo.util;

import android.graphics.Canvas;

/**
 * Created by zhangchao on 18-1-17.
 */

public interface DrawInterface {
    void onDrawToScreen(Canvas canvas);
    void onDrawToPic(Canvas canvas, float scale);
}
