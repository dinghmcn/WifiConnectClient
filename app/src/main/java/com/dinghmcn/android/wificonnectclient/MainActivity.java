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
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.dinghmcn.android.wificonnectclient.utils.CheckPermissionUtils;
import com.dinghmcn.android.wificonnectclient.utils.ConnectManager;
import com.google.android.cameraview.CameraView;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_SCANNER_CODE = 8;
    public static final int REQUEST_CAMERA_CODE = 9;
    /**
     * 请求CAMERA权限码
     */
    private static final int REQUEST_CAMERA_PERM = 101;


    private ScrollView mScrollView;
    private TextView mTextView;
    private Button mButton;

    private StringBuilder mConnectMessage;

    private Handler mMainHandler;
    private ConnectManager mConnectManager = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrollView = findViewById(R.id.message_scrollview);
        mTextView = findViewById(R.id.connect_message);
        mButton = findViewById(R.id.clean_message);
        mButton.setOnClickListener(this);

        mConnectMessage = new StringBuilder();

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ConnectManager.CONNECT_FAILED:
                        outPutLog(R.string.connect_failed);
                        break;
                    case ConnectManager.CONNECT_CLOSED:
                        outPutLog(R.string.connect_closed);
                        break;
                    case ConnectManager.CONNECT_SUCCESS:
                        outPutLog(R.string.connect_success);
                        break;
                    case ConnectManager.COMMAND_ERROR:
                        outPutLog(R.string.command_error);
                        break;
                    case ConnectManager.COMMAND_RECEIVE:
                        outPutLog(R.string.wait_command);
                        break;
                    case ConnectManager.COMMAND_TAKE_PHOTO_FRONT:
                        final Intent intent0 = new Intent(MainActivity.this,
                                CameraActivity.class)
                                .putExtra("camera_id", CameraView.FACING_FRONT);
                        startActivityForResult(intent0, REQUEST_CAMERA_CODE);
                        break;
                    case ConnectManager.COMMAND_TAKE_PHOTO_BACK:
                        final Intent intent1 = new Intent(MainActivity.this,
                                CameraActivity.class)
                                .putExtra("camera_id", CameraView.FACING_BACK);
                        startActivityForResult(intent1, REQUEST_CAMERA_CODE);
                        break;
                        default:
                            outPutLog(Integer.toString(msg.what));
                            if (null != mConnectManager && ConnectManager.mConnected) {
                                mConnectManager.sendMessageToServer(Integer.toString(msg.what));
                            }
                }
            }
        };

        ZXingLibrary.initDisplayOpinion(this);
        initPermission();
        cameraTask();

        /*final Intent intent1 = new Intent(MainActivity.this,
                CameraActivity.class).putExtra("camera_id", CameraView.FACING_FRONT);
        startActivityForResult(intent1, REQUEST_CAMERA_CODE);*/

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "result:" + requestCode + "/" + resultCode);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SCANNER_CODE:
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
                    break;
                case REQUEST_CAMERA_CODE:
                    if (null != data) {
                        Uri pictureUri = data.getData();
                        if (null != pictureUri && ConnectManager.mConnected) {
                            mConnectManager.sendFileToServer(pictureUri);
                            outPutLog(getString(R.string.send_file, pictureUri.toString()));
                        }

                    }
                    break;

                    default:
            }
        } else {
            outPutLog(R.string.execute_command_error);
            Log.e(TAG, "return result failed.");
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
     * EsayPermissions接管权限处理逻辑
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


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

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsGranted()...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsDenied()...", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, "当前App需要申请camera权限,需要打开设置页面么?")
                    .setTitle("权限申请")
                    .setPositiveButton("确认")
                    .setNegativeButton("取消", null)
                    .setRequestCode(REQUEST_CAMERA_PERM)
                    .build()
                    .show();
        }
    }

    /**
     * 初始化权限事件
     */
    private void initPermission() {
        Log.d(TAG, "check permissions");
        //检查权限
        String[] permissions = CheckPermissionUtils.checkPermission(this);
        if (permissions.length == 0) {
            //权限都申请了
            Log.d(TAG, "permission all");
        } else {
            Log.d(TAG, "request permissions : " + Arrays.toString(permissions));
            //申请权限
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.clean_message) {
            cleanLog();
        }
    }

    private void prepareConnectServer(String ipAndPort) {
        Log.d(TAG, "Prepare connect server.");
        String serverIp = "";
        int serverPort = -1;
        String serverPortStr = "";
        if (ipAndPort != null && !ipAndPort.isEmpty()) {
            String [] tmpServerIPAndPort = ipAndPort.split(":");
            serverIp = tmpServerIPAndPort[0];
            serverPortStr = tmpServerIPAndPort[1];
        }
        if (!ConnectManager.isIP(serverIp) || !ConnectManager.isInteger(serverPortStr)) {
            outPutLog(ipAndPort + ":" + getString(R.string.ip_or_port_illegal));
            serverIp = "";
            serverPort = -1;
        } else {
            serverPort = Integer.parseInt(serverPortStr);
            InetSocketAddress inetSocketAddress= new InetSocketAddress(serverIp, serverPort);
            mConnectManager = ConnectManager.newInstance(mMainHandler, inetSocketAddress);
            mConnectManager.connectServer();
            outPutLog(getString(R.string.connect_loading, ipAndPort));
        }
    }

    @Override
    protected void onDestroy() {
        ConnectManager.mConnected =false;
        if (null != mConnectManager) {
            mConnectManager.disconnectServer();
            mConnectManager = null;
        }
        super.onDestroy();
    }
}
