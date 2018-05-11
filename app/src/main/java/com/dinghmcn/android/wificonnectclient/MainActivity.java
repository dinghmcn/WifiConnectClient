package com.dinghmcn.android.wificonnectclient;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dinghmcn.android.wificonnectclient.utils.CheckPermissionUtils;
import com.dinghmcn.android.wificonnectclient.utils.ConnectManagerUtils;
import com.dinghmcn.android.wificonnectclient.utils.ConnectManagerUtils.EnumCommand;
import com.dinghmcn.android.wificonnectclient.utils.SensorManagerUtils;
import com.dinghmcn.android.wificonnectclient.utils.TimeUtils;
import com.dinghmcn.android.wificonnectclient.utils.WifiManagerUtils;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * main.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
    View.OnClickListener {

  /**
   * The constant REQUEST_SCANNER_CODE.
   */
  public static final int REQUEST_SCANNER_CODE = 8;
  /**
   * The constant REQUEST_CAMERA_CODE.
   */
  public static final int REQUEST_CAMERA_CODE = 9;
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String COMPILE_DATE = "2018-04-26";
  private static final int EXPIRED_DAYS = 7;
  /**
   * 请求CAMERA权限码.
   */
  private static final int REQUEST_CAMERA_PERM = 101;

  private ScrollView mScrollView;
  private TextView mTextView;
  private Button mButton;

  private StringBuilder mConnectMessage;

  private Handler mMainHandler;
  private ConnectManagerUtils mConnectManager = null;
  private WifiManagerUtils mWifiManagerUtils = null;


  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
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

    if (TimeUtils.isExpired(this, COMPILE_DATE, EXPIRED_DAYS)) {
      outPutLog(R.string.software_expired);
      return;
    }

    initPermission();
    ZXingLibrary.initDisplayOpinion(this);

  }

  /**
   * On activity result.
   *
   * @param requestCode the request code
   * @param resultCode  the result code
   * @param data        the data
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "result:" + requestCode + "/" + resultCode);
    switch (requestCode) {
      case REQUEST_SCANNER_CODE:
        if (resultCode == RESULT_OK) {
          if (null != data) {
            Bundle bundle = data.getExtras();
            if (null == bundle) {
              return;
            }

            if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
              String result = bundle.getString(CodeUtils.RESULT_STRING);
              if (result != null && !result.isEmpty()) {
                prepareConnectServer(result);
              } else {
                outPutLog(R.string.get_ip_failed);
              }
            } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
              outPutLog(R.string.get_ip_failed);
            }

          }
        } else {
          outPutLog(R.string.get_ip_failed);
        }
        break;
      case REQUEST_CAMERA_CODE:
        if (resultCode == RESULT_OK) {
          if (null != data) {
            Uri pictureUri = data.getData();
            if (null != pictureUri && ConnectManagerUtils.mConnected) {
              mConnectManager.sendFileToServer(pictureUri);
              outPutLog(getString(R.string.send_file, pictureUri.toString()));
            }

          }
        } else {
          outPutLog(R.string.execute_command_error);
          Log.e(TAG, "return result failed.");
        }
        break;
      default:
    }
  }

  private void outPutLog(String message) {
    mConnectMessage.append(message).append("\r\n");
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
   * EsayPermissions接管权限处理逻辑.
   *
   * @param requestCode  the request code
   * @param permissions  the permissions
   * @param grantResults the grant results
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Forward results to EasyPermissions
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }


  /**
   * Camera task.
   */
  @AfterPermissionGranted(REQUEST_CAMERA_PERM)
  public void cameraTask() {
    if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
      // Have permission, do the thing!
      Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
      startActivityForResult(intent, REQUEST_SCANNER_CODE);
    } else {
      // Ask for one permission
      EasyPermissions.requestPermissions(this, "需要请求camera权限",
          REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);
    }
  }

  /**
   * On permissions granted.
   *
   * @param requestCode the request code
   * @param perms       the perms
   */
  @Override
  public void onPermissionsGranted(int requestCode, List<String> perms) {
    cameraTask();
  }

  /**
   * On permissions denied.
   *
   * @param requestCode the request code
   * @param perms       the perms
   */
  @Override
  public void onPermissionsDenied(int requestCode, List<String> perms) {
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      new AppSettingsDialog.Builder(this,
          "当前App需要申请camera权限,需要打开设置页面么?")
          .setTitle("权限申请").setPositiveButton("确认")
          .setNegativeButton("取消", null)
          .setRequestCode(REQUEST_CAMERA_PERM).build().show();
    }
  }

  /**
   * 初始化权限事件.
   */
  private void initPermission() {
    Log.d(TAG, "check permissions");
    //检查权限
    String[] permissions = CheckPermissionUtils.checkPermission(this);
    if (permissions.length == 0) {
      //权限都申请了
      cameraTask();
      Log.d(TAG, "permission all");
    } else {
      Log.d(TAG, "request permissions : " + Arrays.toString(permissions));
      //申请权限
      ActivityCompat.requestPermissions(this, permissions, 100);
    }
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
    JSONObject jsonObject = new JSONObject();
    Message message = Message.obtain();
    message.what = EnumCommand.FUNCTION.ordinal();
    try {
      jsonObject.putOpt("Key", KeyEvent.keyCodeToString(keyCode));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    message.obj = jsonObject;
    mMainHandler.sendMessage(message);
    return true;
  }

  /**
   * On touch event boolean.
   *
   * @param event the event
   * @return the boolean
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {

    return true;
  }

  /**
   * On click.
   *
   * @param v the v
   */
  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.clean_message) {
      cleanLog();
    }
  }

  private void prepareConnectServer(String connectInfo) {
    Log.d(TAG, "Prepare connect server.");
    if (connectInfo != null && !connectInfo.isEmpty()) {
      JSONObject jsonObject = null;
      try {
        jsonObject = new JSONObject(connectInfo);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      String serverIp = jsonObject.optString("SSID", "");
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
    public void handleMessage(Message msg) {
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
            default:
          }
          break;

        case CAMERA:
          final Intent intent20 = new Intent(mainActivity, CameraActivity.class)
              .putExtra("camera_parameter", msg.obj.toString());
          mainActivity.startActivityForResult(intent20, REQUEST_CAMERA_CODE);
          break;
        case SHOW_PICTURE:
          String imageName;
          JSONObject jsonObject = (JSONObject) msg.obj;
          imageName = jsonObject.optString("FileName", "");
          int resId = mainActivity.getResources().getIdentifier(imageName, "drawable",
              mainActivity.getPackageName());
          if (resId > 0) {
            final Intent intent30 = new Intent(mainActivity, ShowPictureFullActivity.class)
                .putExtra("res_id", resId);
            mainActivity.startActivity(intent30);
            mainActivity.outPutLog(mainActivity.getString(R.string.show_file, imageName));
            Log.d(TAG, mainActivity.getString(R.string.show_file, imageName));
          } else {
            mainActivity.outPutLog(mainActivity.getString(R.string.file_not_exist, imageName));
            Log.d(TAG, mainActivity.getString(R.string.file_not_exist, imageName));
          }
          break;
        case SENSOR:
          final SensorManagerUtils sensorManagerUtils =
              SensorManagerUtils.getInstance(mainActivity);
          postDelayed(new Runnable() {
            @Override
            public void run() {
              mainActivity.mConnectManager.sendMessageToServer(
                  sensorManagerUtils.getJSONObject().toString());
            }
          }, 1000);
          break;
        case FUNCTION:
          mainActivity.mConnectManager.sendMessageToServer(msg.obj.toString());
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
