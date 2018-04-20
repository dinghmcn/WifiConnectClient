package com.dinghmcn.android.wificonnectclient.utils;

import org.xmlpull.v1.XmlPullParser;

import java.util.regex.Pattern;

/**
 * @author dinghmcn
 * @date 2018/4/20 10:47
 **/
public class ConnectUtil {
    /**
     * 判断IP地址的合法性，这里采用了正则表达式的方法来判断
     * return true，合法
     * */
    public static boolean isIP(String ipAddr) {
        if (null != ipAddr && !ipAddr.isEmpty()) {
            // 定义正则表达式
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            // 判断ip地址是否与正则表达式匹配
            if (ipAddr.matches(regex)) {
                // 返回判断信息
                return true;
            } else {
                // 返回判断信息
                return false;
            }
        }
        return false;
    }

    public static boolean isInteger(String str) {
        if (null == str || str.isEmpty()) {
            return false;
        }
        String regex = "^[-\\+]?[\\d]*$";
        return str.matches(regex);
    }
}
