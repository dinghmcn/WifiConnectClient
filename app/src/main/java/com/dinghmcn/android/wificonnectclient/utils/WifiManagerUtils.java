package com.dinghmcn.android.wificonnectclient.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

/**
 * @author dinghmcn
 * @date 2018/4/25 16:09
 **/
public class WifiManagerUtils {
  private static final String TAG = "WifiManagerUtils";

  private static final int WIFICIPHER_NOPASS = 0;
  private static final int WIFICIPHER_WEP = 1;
  private static final int WIFICIPHER_WPA = 2;

  private static WifiManagerUtils instance = null;

  private static Context mContext;
  private static WifiManager mWifiManager;

  private WifiManagerUtils(Context context) {
    mContext = context;
    mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
  }

  public static WifiManagerUtils getInstance(Context context) {
    if (instance == null) {
      instance = new WifiManagerUtils(context);
    }
    return instance;
  }

  public boolean connectWifi(String ssid, String password) {
    Log.d(TAG, "SSID:" + ssid + " password:" + password);

    //如果之前有类似的配置
    WifiConfiguration tempConfig = isExist(ssid);
    if (tempConfig != null) {
      //则清除旧有配置
      int netId = mWifiManager.updateNetwork(createWifiConfig(ssid, password, getType(ssid), tempConfig.networkId));
      Log.d(TAG, "netId1:" + netId);
      return mWifiManager.enableNetwork(netId, true);
    } else {
      int netId = mWifiManager.addNetwork(createWifiConfig(ssid, password, getType(ssid)));
      Log.d(TAG, "netId2:" + netId);
      return mWifiManager.enableNetwork(netId, true);
    }
  }

  public boolean isWifiEnabled() {
    Log.d(TAG, "isWifiEnabled():" + mWifiManager.isWifiEnabled());
    return mWifiManager.isWifiEnabled();
  }

  public boolean isWifiConnected(String ssid) {
    ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetInfo.isConnected()) {
      Log.d(TAG, activeNetInfo.toString());
      Log.d(TAG, "isWifiConnected():" + ('"' + ssid + '"').equals(activeNetInfo.getExtraInfo()));
      return ('"' + ssid + '"').equals(activeNetInfo.getExtraInfo());
    }
    Log.d(TAG, "isWifiConnected():false");
    return false;
  }

  public void openWifi() {
    mWifiManager.setWifiEnabled(true);
  }

  private WifiConfiguration createWifiConfig(String ssid, String password, int type) {
    //初始化WifiConfiguration
    WifiConfiguration config = new WifiConfiguration();
    config.allowedAuthAlgorithms.clear();
    config.allowedGroupCiphers.clear();
    config.allowedKeyManagement.clear();
    config.allowedPairwiseCiphers.clear();
    config.allowedProtocols.clear();

    //指定对应的SSID
    config.SSID = "\"" + ssid + "\"";

    //不需要密码的场景
    if (type == WIFICIPHER_NOPASS) {
      config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
      //以WEP加密的场景
    } else if (type == WIFICIPHER_WEP) {
      config.hiddenSSID = true;
      config.wepKeys[0] = "\"" + password + "\"";
      config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
      config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
      config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
      config.wepTxKeyIndex = 0;
      //以WPA加密的场景，自己测试时，发现热点以WPA2建立时，同样可以用这种配置连接
    } else if (type == WIFICIPHER_WPA) {
      config.preSharedKey = "\"" + password + "\"";
      config.hiddenSSID = true;
      config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
      config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
      config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
      config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
      config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
      config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
      config.status = WifiConfiguration.Status.ENABLED;
    }
    return config;
  }

  private WifiConfiguration createWifiConfig(String ssid, String password, int type, int netId) {
    WifiConfiguration wifiConfiguration = createWifiConfig(ssid, password, type);
    wifiConfiguration.networkId = netId;
    return wifiConfiguration;
  }

  @Nullable
  private WifiConfiguration isExist(String ssid) {
    List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

    for (WifiConfiguration config : configs) {
      if (config.SSID.equals("\"" + ssid + "\"")) {
        return config;
      }
    }
    return null;
  }

  private int getType(String ssid) {

    List<ScanResult> mScanResultList = mWifiManager.getScanResults();

    for (ScanResult scanResult : mScanResultList) {
      if (ssid.equals(scanResult.SSID)) {
        if (scanResult.capabilities.contains("WPA")) {
          return WIFICIPHER_WPA;
        } else if (scanResult.capabilities.contains("WEP")) {
          return WIFICIPHER_WEP;
        } else {
          return WIFICIPHER_NOPASS;
        }
      } else {
        Log.d(TAG, ssid + "isn't discover.");
      }
    }

    return WIFICIPHER_WPA;
  }

}
