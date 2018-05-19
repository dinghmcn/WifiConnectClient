package com.dinghmcn.android.wificonnectclient;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dinghmcn.android.wificonnectclient.utils.ConnectManagerUtils;
import com.dinghmcn.android.wificonnectclient.utils.ConnectManagerUtils.EnumCommand;
import com.dinghmcn.android.wificonnectclient.utils.SensorManagerUtils;
import com.dinghmcn.android.wificonnectclient.utils.TimeUtils;
import com.dinghmcn.android.wificonnectclient.utils.WifiManagerUtils;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * main.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

  /**
   * The constant REQUEST_CAMERA_CODE.
   */
  public static final int REQUEST_CAMERA_CODE = 9;
  private static final String COMPILE_DATE = "2018-05-18";
  private static final int EXPIRED_DAYS = 15;
  private static final int REQUEST_FUNCTION_FINGER = 102;
  private static boolean isReleased = false;
  private static boolean isCatchKey = false;
  private static boolean isCatchTouch = false;
  @Nullable
  private static JSONObject mKeyJsonObject;
  @Nullable
  private static JSONObject mTouchJsonObject;
  @Nullable
  private static JSONArray mTouchJsonArray;
  private ScrollView mScrollView;
  private TextView mTextView;
  private Button mButton;

  private StringBuilder mConnectMessage;

  private Handler mMainHandler;
  @Nullable
  private ConnectManagerUtils mConnectManager = null;
  @Nullable
  private WifiManagerUtils mWifiManagerUtils = null;


  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (!isReleased && TimeUtils.isExpired(this, COMPILE_DATE, EXPIRED_DAYS)) {
      outPutLog(R.string.software_expired);
      return;
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mScrollView = findViewById(R.id.message_scrollview);
    mTextView = findViewById(R.id.connect_message);
    mButton = findViewById(R.id.clean_message);
    mButton.setOnClickListener(this);

    mConnectMessage = new StringBuilder();

    mMainHandler = new MainHandel(this);

    mWifiManagerUtils = WifiManagerUtils.getInstance(this);
    if (!mWifiManagerUtils.isWifiEnabled()) {
      mWifiManagerUtils.openWifi();
    }
  }

  /**
   * On activity result.
   *
   * @param requestCode the request code
   * @param resultCode  the result code
   * @param data        the data
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "result:" + requestCode + "/" + resultCode);
    if (requestCode == REQUEST_CAMERA_CODE) {
      if (resultCode == RESULT_OK && null != data) {
        Uri pictureUri = data.getData();
        if (null != pictureUri && ConnectManagerUtils.mConnected) {
          mConnectManager.sendFileToServer(pictureUri);
          outPutLog(getString(R.string.send_file, pictureUri.toString()));
        }
      } else {
        outPutLog(R.string.execute_command_error);
        Log.e(TAG, "return result failed.");
      }
    }
  }

  private void outPutLog(String message) {
    mConnectMessage.append(message)
        .append("\r\n");
    mTextView.setText(mConnectMessage);
    mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
  }

  private void outPutLog(int idRes) {
    outPutLog(getString(idRes));
  }

  private void cleanLog() {
    mConnectMessage.setLength(0);
    mTextView.setText(mConnectMessage.toString());
  }

  /**
   * On key down boolean.
   *
   * @param keyCode the key code
   * @param event   the event
   * @return the boolean
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (isCatchKey) {
      outPutLog(keyCode + "|" + KeyEvent.keyCodeToString(keyCode));
      if (mKeyJsonObject == null) {
        mKeyJsonObject = new JSONObject();
      }
      try {
        mKeyJsonObject.putOpt(KeyEvent.keyCodeToString(keyCode), keyCode);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  /**
   * Dispatch touch event boolean.
   *
   * @param ev the ev
   * @return the boolean
   */
  @Override
  public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
    Log.d(TAG, "onTouchEvent");
    if (isCatchTouch) {
      if (mTouchJsonArray == null) {
        mTouchJsonArray = new JSONArray();
      }
      switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
          Log.d(TAG, "down");
          mTouchJsonObject = new JSONObject();
          try {
            mTouchJsonObject.put("DOWN", "(" + ev.getRawX() + "," + ev.getRawY() + ")");
          } catch (JSONException e) {
            e.printStackTrace();
          }
          break;
        case MotionEvent.ACTION_MOVE:
          Log.d(TAG, "move");
          try {
            mTouchJsonObject.put("MOVE" + (mTouchJsonObject.length() - 1),
                "(" + ev.getRawX() + "," + ev.getRawY() + ")");
          } catch (JSONException e) {
            e.printStackTrace();
          }
          break;
        case MotionEvent.ACTION_UP:
          Log.d(TAG, "up");
          try {
            mTouchJsonObject.put("UP", "(" + ev.getRawX() + "," + ev.getRawY() + ")");
          } catch (JSONException e) {
            e.printStackTrace();
          }
          mTouchJsonArray.put(mTouchJsonObject);
          Log.d(TAG, mTouchJsonObject.toString() + " | " + mTouchJsonArray.toString());
          mTouchJsonObject = null;
          break;
        default:
      }
      return true;
    }
    return super.dispatchTouchEvent(ev);
  }

  /**
   * On click.
   *
   * @param v the v
   */
  @Override
  public void onClick(@NonNull View v) {
    if (v.getId() == R.id.clean_message) {
      cleanLog();
    }
  }

  /**
   * Scanner result.
   *
   * @param result the result
   */
  @Override
  protected void scannerResult(String result) {
    if (null != result && !result.isEmpty()) {
      prepareConnectServer(result);
    } else {
      outPutLog(R.string.get_ip_failed);
    }
  }

  private void prepareConnectServer(@Nullable String connectInfo) {
    Log.d(TAG, "Prepare connect server.");
    if (connectInfo != null && !connectInfo.isEmpty()) {
      JSONObject jsonObject = null;
      try {
        jsonObject = new JSONObject(connectInfo);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      String serverIp = jsonObject.optString("IP", "");
      int serverPort = jsonObject.optInt("Port", -1);
      String wifiSsid = jsonObject.optString("SSID", "");
      String wifiPassword = jsonObject.optString("PWD", "");
      int station = jsonObject.optInt("Station", -1);


      String[] connectInfos = connectInfo.split(":");
      if (ConnectManagerUtils.isIp(serverIp) && serverPort > 0) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
        mConnectManager = ConnectManagerUtils.newInstance(mMainHandler, inetSocketAddress);
        mConnectManager.connectServer(mWifiManagerUtils, wifiSsid, wifiPassword);
        outPutLog(getString(R.string.connect_loading, inetSocketAddress.toString()));
      } else {
        outPutLog(serverIp + ":" + serverPort + getString(R.string.ip_or_port_illegal));
      }
    } else {
      outPutLog(connectInfo + " " + getString(R.string.connect_info_error));
    }
  }

  /**
   * On destroy.
   */
  @Override
  protected void onDestroy() {
    ConnectManagerUtils.mConnected = false;
    if (null != mConnectManager) {
      mConnectManager.disconnectServer();
      mConnectManager = null;
    }
    super.onDestroy();
  }

  /**
   * On back pressed.
   */
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
  }

  private static class MainHandel extends Handler {
    /**
     * The M activity weak reference.
     */
    WeakReference<MainActivity> mActivityWeakReference;

    /**
     * Instantiates a new Main handel.
     *
     * @param activity the activity
     */
    MainHandel(MainActivity activity) {
      mActivityWeakReference = new WeakReference<MainActivity>(activity);
    }

    /**
     * Handle message.
     *
     * @param msg the msg
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
      final MainActivity mainActivity = mActivityWeakReference.get();
      switch (EnumCommand.values()[msg.what]) {
        case CONNECT:
          int connect = msg.arg1;
          switch (connect) {
            case ConnectManagerUtils.CONNECT_FAILED:
              mainActivity.outPutLog(R.string.connect_failed);
              break;
            case ConnectManagerUtils.CONNECT_CLOSED:
              mainActivity.outPutLog(R.string.connect_closed);
              break;
            case ConnectManagerUtils.CONNECT_SUCCESS:
              mainActivity.outPutLog(R.string.connect_success);
              break;
            default:
          }
          break;
        case COMMAND:
          int command = msg.arg1;
          switch (command) {
            case ConnectManagerUtils.COMMAND_ERROR:
              mainActivity.outPutLog(R.string.command_error);
              break;
            case ConnectManagerUtils.COMMAND_RECEIVE:
              mainActivity.outPutLog(R.string.wait_command);
              break;
            case ConnectManagerUtils.COMMAND_SEND:
              mainActivity.outPutLog(msg.obj.toString());
              break;
            default:
          }
          break;

        case CAMERA:
          final Intent intent20 = new Intent(mainActivity, CameraActivity.class).putExtra(
              "camera_parameter", msg.obj.toString());
          mainActivity.startActivityForResult(intent20, REQUEST_CAMERA_CODE);
          break;
        case SHOW_PICTURE:
          String imageName;
          final JSONObject sJsonObject = (JSONObject) msg.obj;
          imageName = sJsonObject.optString("FileName", "");
          int resId = mainActivity.getResources()
              .getIdentifier(imageName, "drawable",
                  mainActivity.getPackageName());
          if (resId > 0) {
            final Intent intent30 = new Intent(mainActivity,
                ShowPictureFullActivity.class).putExtra("res_id", resId);
            mainActivity.startActivity(intent30);
            mainActivity.outPutLog(mainActivity.getString(R.string.show_file, imageName));
            Log.d(mainActivity.TAG, mainActivity.getString(R.string.show_file, imageName));
          } else {
            mainActivity.outPutLog(mainActivity.getString(R.string.file_not_exist, imageName));
            Log.d(mainActivity.TAG, mainActivity.getString(R.string.file_not_exist, imageName));
          }
          break;
        case SENSOR:
          final SensorManagerUtils sensorManagerUtils = SensorManagerUtils.getInstance(
              mainActivity);
          postDelayed(new Runnable() {
            @Override
            public void run() {
              mainActivity.mConnectManager.sendMessageToServer(
                  sensorManagerUtils.getJSONObject()
                      .toString());
            }
          }, 1000);
          break;
        case FUNCTION:
          JSONObject fJsonObject = (JSONObject) msg.obj;
          String content = fJsonObject.optString("Content");
          final int time = fJsonObject.optInt("Time", 8) * 1000;
          switch (content) {
            case "Key":
              isCatchKey = true;
              postDelayed(new Runnable() {
                @Override
                public void run() {
                  if (mKeyJsonObject != null) {
                    mainActivity.mConnectManager.sendMessageToServer(mKeyJsonObject.toString());
                  }
                  mKeyJsonObject = null;
                  isCatchKey = false;
                }
              }, time);
              break;
            case "Touch":
              isCatchTouch = true;
              postDelayed(new Runnable() {
                @Override
                public void run() {
                  if (mTouchJsonArray != null) {
                    mainActivity.mConnectManager.sendMessageToServer(mTouchJsonArray.toString());
                  }
                  mTouchJsonArray = null;
                  isCatchTouch = false;
                }
              }, time);
              break;
            case "Finger":
              break;
            default:
          }
          break;
        default:
          mainActivity.outPutLog(Integer.toString(msg.what));
          if (null != mainActivity.mConnectManager && ConnectManagerUtils.mConnected) {
            mainActivity.mConnectManager.sendMessageToServer(Integer.toString(msg.what));
          }
      }
    }
  }
}
