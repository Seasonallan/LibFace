package com.library.aimo.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.library.aimo.util.ImoLog;

public class DeviceRotationDetector implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float x, y, z;
    private Context mContext;
    private boolean disable;

    public DeviceRotationDetector(Context context) {
        mContext = context;
    }

    public void onResume() {
        if(!disable) {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            boolean registered = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            ImoLog.d("GRAVITY", "gravity sensor registered: "+registered);
            if (!registered) {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                registered = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                ImoLog.d("GRAVITY", "accelerometer registered: "+registered);
            }
        }
    }

    public void onPause() {
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this, mSensor);
            mSensorManager = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        x = sensorEvent.values[0];
        y = sensorEvent.values[1];
        z = sensorEvent.values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 顺时针旋转角度扭正
     *
     * @return
     */
    public int getRotationDegree() {
        if (Math.abs(y) >= Math.abs(x)) {
            if (y >= 0) {
                return 0;
            } else {
                return 180;
            }
        } else {
            if (x >= 0) {
                return 90;
            } else {
                return 270;
            }
        }
    }

    public void disable(boolean disable) {
        this.disable = disable;
    }
}
