package com.dinghmcn.android.wificonnectclient;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.dinghmcn.android.wificonnectclient.utils.CheckPermissionUtils;
import com.dinghmcn.android.wificonnectclient.utils.ConnectUtil;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private final int REQUEST_CODE = 8;
    /**
     * 请求CAMERA权限码
     */
    private static final int REQUEST_CAMERA_PERM = 101;

    private static final int CONNECT_FAILED = -1;
    private static final int CONNECT_CLOSED = 0;
    private static final int CONNECT_SUCCESS = 1;
    private static final int CCOMMAND_RECEIVE = 2;
    private static final int CCOMMAND_RED = 31;
    private static final int COMMAND_GREEN = 32;
    private static final int COMMAND_BLUE = 33;
    private static final int COMMAND_ERROR = 34;

    private static boolean mConnected = false;

    private ScrollView mScrollView;
    private TextView mTextView;
    private Button mButton;

    private StringBuilder mConnectMessage;
    private String mServerIp;
    private int mServerPort;

    private Handler mMainHandler;
    private Socket mSocket;
    private ExecutorService mThreadPool;

    InputStream is;
    OutputStream out;
    InputStreamReader isr;
    BufferedReader br;
    String response;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrollView = findViewById(R.id.message_scrollview);
        mTextView = findViewById(R.id.connect_message);
        mButton = findViewById(R.id.clean_message);
        mButton.setOnClickListener(this);

        mConnectMessage = new StringBuilder();

        mThreadPool =  Executors.newCachedThreadPool();

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECT_FAILED:
                        outPutLog(R.string.connect_failed);
                        break;
                    case CONNECT_SUCCESS:
                        outPutLog(R.string.connect_success);
                        break;
                    case COMMAND_ERROR:
                        outPutLog(R.string.command_error);
                        break;
                    case CCOMMAND_RECEIVE:
                        outPutLog(R.string.wait_command);
                        break;
                        default:
                            outPutLog(Integer.toString(msg.what));
                            response = Integer.toString(msg.what);
                            sendMessageToServer();

                }
            }
        };

        ZXingLibrary.initDisplayOpinion(this);
        initPermission();
        cameraTask();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "result:" + requestCode + "/" + resultCode);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
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
            finish();
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
            startActivityForResult(intent, REQUEST_CODE);
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
        String serverPortStr = "";
        if (ipAndPort != null && !ipAndPort.isEmpty()) {
            String [] tmpServerIPAndPort = ipAndPort.split(":");
            mServerIp = tmpServerIPAndPort[0];
            serverPortStr = tmpServerIPAndPort[1];
        }
        if (!ConnectUtil.isIP(mServerIp) || !ConnectUtil.isInteger(serverPortStr)) {
            outPutLog(ipAndPort + ":" + getString(R.string.ip_or_port_illegal));
            mServerIp = "";
            mServerPort = -1;
        } else {
            mServerPort = Integer.parseInt(serverPortStr);
            connectServer();
            outPutLog(getString(R.string.connect_loading, ipAndPort));
        }
    }

    private void connectServer() {
        Log.d(TAG, "Connect server.");
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(mServerIp, mServerPort);
                    Log.d(TAG, "Connected :" + mSocket.isConnected());
                    if ( mSocket.isConnected()) {
                        mConnected = true;
                        receiveMessageFromServer();
                        mMainHandler.sendEmptyMessage(CONNECT_SUCCESS);
                    } else {
                        mMainHandler.sendEmptyMessage(CONNECT_FAILED);
                    }
                } catch (IOException e) {
                    mMainHandler.sendEmptyMessage(CONNECT_FAILED);
                    Log.e(TAG, "Connect server failed!");
                    e.printStackTrace();
                }
            }
        });
    }

    private void receiveMessageFromServer() {
        Log.d(TAG, "Receive message.");
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                mMainHandler.sendEmptyMessage(CCOMMAND_RECEIVE);
                while (mConnected && !mSocket.isClosed() && mSocket.isConnected() && !mSocket.isInputShutdown()) {
                    Log.d(TAG, "Start receive message.");
                    try {
                        is = mSocket.getInputStream();
                        String command;
                        byte[] tempBuffer = new byte[2048];
                        int numReadedBytes = is.read(tempBuffer, 0, tempBuffer.length);
                        command = new String(tempBuffer, 0, numReadedBytes);
                        Log.d(TAG, "Command:" + command);

                        if (ConnectUtil.isInteger(command)) {
                            mMainHandler.sendEmptyMessage(Integer.parseInt(command));
                        } else {

                        }

                    } catch (IOException e) {
                        Log.d(TAG, "Receive message error.");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void sendMessageToServer(){
        Log.d(TAG, "Send message :" + response);
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    out = mSocket.getOutputStream();
                    out.write(response.getBytes());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void disconnectServer() {
        Log.d(TAG, "Disconnect server.");
        try {
            if (null != out) {
                out.close();
            }
            if (null != br) {
                br.close();
            }
            if (null != mSocket) {
                mSocket.close();
                mConnected = false;
            }
            if (mSocket.isClosed() || mSocket.isConnected()) {
                outPutLog(R.string.connect_closed);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnected =false;
        disconnectServer();
    }
}
