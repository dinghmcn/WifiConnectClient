package com.dinghmcn.android.wificonnectclient;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.dinghmcn.android.wificonnectclient.utils.CheckPermissionUtils;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * The type Base activity.
 *
 * @author dinghmcn
 */
public abstract class BaseActivity extends AppCompatActivity implements
    EasyPermissions.PermissionCallbacks {
  /**
   * The constant REQUEST_SCANNER_CODE.
   */
  public static final int REQUEST_SCANNER_CODE = 8;
  /**
   * 请求CAMERA权限码.
   */
  private static final int REQUEST_CAMERA_PERM = 101;
  /**
   * The Tag.
   */
  protected final String TAG = getClass().getSimpleName();

  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "result:" + requestCode + "/" + resultCode);
    String result = "";
    if (requestCode == REQUEST_SCANNER_CODE && resultCode == RESULT_OK && null != data) {
      Bundle bundle = data.getExtras();
      if (null != bundle && bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
        result = bundle.getString(CodeUtils.RESULT_STRING, "");
      }
    }
    scannerResult(result);
  }

  /**
   * Gets scanner result.
   *
   * @param result the result
   * @return the scanner result
   */
  protected abstract void scannerResult(String result);

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
      Intent intent = new Intent(BaseActivity.this, CaptureActivity.class);
      startActivityForResult(intent, REQUEST_SCANNER_CODE);
    } else {
      // Ask for one permission
      EasyPermissions.requestPermissions(this, "需要请求camera权限", REQUEST_CAMERA_PERM,
          Manifest.permission.CAMERA);
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
  public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      new AppSettingsDialog.Builder(this, "当前App需要申请camera权限,需要打开设置页面么?").setTitle(
          "权限申请")
          .setPositiveButton("确认")
          .setNegativeButton("取消",
              null)
          .setRequestCode(
              REQUEST_CAMERA_PERM)
          .build()
          .show();
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
}
