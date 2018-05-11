package com.dinghmcn.android.wificonnectclient.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author dinghmcn
 * @date 2018/4/28 15:35
 **/
public class SensorManagerUtils implements SensorEventListener {
  private static final String TAG = "SensorManagerUtils";

  private static SensorManagerUtils instance = null;
  private final List<Integer> mSensorList = Arrays.asList(Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_PROXIMITY, Sensor.TYPE_LIGHT, Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE);
  private SensorManager mSensorManager;
  private JSONObject mJSONObject = new JSONObject();

  private SensorManagerUtils(Context context) {
    mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    mSensorList.retainAll(mSensorManager.getSensorList(Sensor.TYPE_ALL));
    registerListeners();
  }

  public static SensorManagerUtils getInstance(Context context) {
    if (null == instance) {
      instance = new SensorManagerUtils(context);
    }
    return instance;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (mSensorList.contains(event.sensor.getType())) {
      try {
        mJSONObject.put(event.sensor.getName(), Arrays.toString(event.values));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  private void registerListeners() {
    for (int sensor : mSensorList) {
      mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(sensor), SensorManager.SENSOR_DELAY_UI);
    }
  }

  public JSONObject getJSONObject() {
    return mJSONObject;
  }
}
