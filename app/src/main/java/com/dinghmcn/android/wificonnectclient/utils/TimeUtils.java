package com.dinghmcn.android.wificonnectclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
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
  public static boolean isExpired(Context context, String date, int day) {
    long firstBootTime = getFirstBootTime(context);
    long expiredToFirstBootTime = getFirstBootTime(context) + day * 24 * 60 * 60 * 1000L;
    long localTime = getLocalTime();
    long compileTime = getTimeForString(date);
    long expiredToCompileTime = compileTime + day * 24 * 60 * 60 * 1000L;
    Log.d(TAG, "date:" + date + " day:" + day + " localTime:" + localTime + " firstBootTime:"
        + firstBootTime + " expiredToFirstBootTime:" + expiredToFirstBootTime + " compileTime:"
        + compileTime + " expiredToCompileTime:" + expiredToCompileTime);
    saveFirstBootTime(context);

    boolean isexpiredToFirstBootTime =
        localTime > firstBootTime && localTime < expiredToFirstBootTime;
    boolean isexpiredToCompileTime = localTime > compileTime && localTime < expiredToCompileTime;

    if ((firstBootTime < compileTime) && isexpiredToFirstBootTime) {
      return false;
    } else {
      return firstBootTime <= compileTime || !isexpiredToCompileTime;
    }

  }


  /**
   * Gets local time.
   *
   * @return the local time
   */
  public static long getLocalTime() {
    return Calendar.getInstance()
                   .getTimeInMillis();
  }

  /**
   * Save first boot time.
   *
   * @param context the context
   */
  public static void saveFirstBootTime(Context context) {
    SharedPreferences.Editor editor = context.getSharedPreferences("time",
        Context.MODE_PRIVATE)
                                             .edit();
    editor.putLong("first_boot_time", getLocalTime())
          .commit();
  }

  /**
   * Gets first boot time.
   *
   * @param context the context
   * @return the first boot time
   */
  public static long getFirstBootTime(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences("time",
        Context.MODE_PRIVATE);
    return sharedPreferences.getLong("first_boot_time", getLocalTime());
  }

  /**
   * Gets time for string.
   *
   * @param dateStr the date str
   * @return the time for string
   */
  public static long getTimeForString(String dateStr) {
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
