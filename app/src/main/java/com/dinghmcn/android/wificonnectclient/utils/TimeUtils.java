package com.dinghmcn.android.wificonnectclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * The type Time utils.
 *
 * @author dinghmcn
 * @date 2018 /4/23 13:51
 */
public class TimeUtils {
  private static final String TAG = "TimeUtils";

  private static final String BAIDU_URL = "http://www.baidu.com";
  private static final String NTSC_URL = "http://www.ntsc.ac.cn";
  private static final String BJTIME_URL = "http://www.bjtime.com";

  private TimeUtils() {
  }

  /**
   * Is expired boolean.
   *
   * @param context the context
   * @param date    the date
   * @param day     the day
   * @return the boolean
   */
  public static boolean isNotExpired(@NonNull Context context, String date, int day) {
    if (day <= 0) {
      return false;
    }
    long firstBootTime = getBootTime(context, "first");
    long lastBootTime = getBootTime(context, "last");
    long localTime = getLocalTime();
    long compileTime = getTimeForString(date);
    long expiredToCompileTime = compileTime + day * 24 * 60 * 60 * 1000L;

    if (firstBootTime < 0) {
      firstBootTime = lastBootTime = localTime;
      saveBootTime(context, "first", localTime);
    }
    long expiredToFirstBootTime = firstBootTime + day * 24 * 60 * 60 * 1000L;

    Log.d(TAG, "date:" + date + " day:" + day + " localTime:" + localTime
        + "lastBootTime" + lastBootTime + " firstBootTime:" + firstBootTime
        + " expiredToFirstBootTime:" + expiredToFirstBootTime + " compileTime:"
        + compileTime + " expiredToCompileTime:" + expiredToCompileTime);

    if (lastBootTime <= localTime) {
      saveBootTime(context, "last", localTime);
    } else {
      return false;
    }

    return (firstBootTime < compileTime && firstBootTime <= localTime
        && localTime <= expiredToFirstBootTime)
        || (compileTime <= localTime && localTime <= expiredToCompileTime);

  }


  /**
   * Gets local time.
   *
   * @return the local time
   */
  private static long getLocalTime() {
    return Calendar.getInstance()
        .getTimeInMillis();
  }

  /**
   * Save first boot time.
   *
   * @param context the context
   */
  private static void saveBootTime(Context context, String key, Long value) {
    SharedPreferences.Editor editor = context.getSharedPreferences("time", Context.MODE_PRIVATE)
        .edit();
    editor.putLong(key, value).apply();
  }

  /**
   * Gets first boot time.
   *
   * @param context the context
   * @return the first boot time
   */
  private static long getBootTime(Context context, String key) {
    SharedPreferences sharedPreferences = context.getSharedPreferences("time",
        Context.MODE_PRIVATE);
    return sharedPreferences.getLong(key, -1);
  }

  /**
   * Gets time for string.
   *
   * @param dateStr the date str
   * @return the time for string
   */
  private static long getTimeForString(String dateStr) {
    String pattern = "yyyy-MM-dd";
    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
    try {
      Date date = dateFormat.parse(dateStr);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      return calendar.getTimeInMillis();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

}
