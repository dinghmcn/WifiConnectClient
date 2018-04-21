package com.dinghmcn.android.wificonnectclient;

import android.util.Log;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author dinghmcn
 * @date 2018/4/20 10:47
 **/
public class ConnectManager {
    private static final String TAG = ConnectManager.class.getSimpleName();

    static final int CONNECT_FAILED = -1;
    static final int CONNECT_CLOSED = 0;
    static final int CONNECT_SUCCESS = 1;
    static final int COMMAND_RECEIVE = 2;
    static final int COMMAND_SEND = 3;
    static final int COMMAND_ERROR = 4;
    static final int COMMAND_GET_SENSORS = 10;
    static final int COMMAND_TAKE_PHOTO_FRONT = 20;
    static final int COMMAND_TAKE_PHOTO_BACK = 21;
    static final int COMMAND_SHOW_PICTURE = 30;
    static final int COMMAND_GREEN = 32;

    static boolean mConnected = false;

    private Handler mMainHandler;

    private InetSocketAddress mInetSocketAddress;
    private Socket mSocket;
    private ExecutorService mThreadPool;

    private InputStream is;
    private OutputStream out;
    private InputStreamReader isr;
    private BufferedReader br;

    private String mMessage = "";

    private static ConnectManager instance = null;

    public static ConnectManager newInstance(Handler handler, InetSocketAddress inetSocketAddress){
        if (null == instance) {
            instance = new ConnectManager(handler, inetSocketAddress);
        }
        return instance;
    }

    private ConnectManager(Handler handler, InetSocketAddress inetSocketAddress) {
        mMainHandler = handler;
        mInetSocketAddress = inetSocketAddress;

        mThreadPool =  Executors.newCachedThreadPool();
    }


    void connectServer() {
        Log.d(TAG, "Connect server.");
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket();
                    mSocket.connect(mInetSocketAddress, 1000);
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

    void receiveMessageFromServer() {
        Log.d(TAG, "Receive message.");
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                mMainHandler.sendEmptyMessage(COMMAND_RECEIVE);
                while (mConnected && !mSocket.isClosed() && mSocket.isConnected() && !mSocket.isInputShutdown()) {
                    Log.d(TAG, "Start receive message.");
                    try {
                        is = mSocket.getInputStream();
                        String command = "";
                        byte[] tempBuffer = new byte[2048];
                        int numReadedBytes = is.read(tempBuffer, 0, tempBuffer.length);
                        if (numReadedBytes > 0) {
                            command = new String(tempBuffer, 0, numReadedBytes);
                        } else {
                            continue;
                        }
                        Log.d(TAG, "Command:" + command);

                        if (ConnectManager.isInteger(command)) {
                            mMainHandler.sendEmptyMessage(Integer.parseInt(command));
                        } else {
                            mMainHandler.sendEmptyMessage(COMMAND_ERROR);
                        }

                    } catch (IOException e) {
                        Log.d(TAG, "Receive message error.");
                        e.printStackTrace();
                    }
                }
                mMainHandler.sendEmptyMessage(CONNECT_CLOSED);
            }
        });
    }

    void sendMessageToServer(String message){
        mMessage = message;
        Log.d(TAG, "Send message :" + mMessage);
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    out = mSocket.getOutputStream();
                    out.write(mMessage.getBytes("UTF-8"));
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void disconnectServer() {
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
                mMainHandler.sendEmptyMessage(CONNECT_CLOSED);
            }
            if (null != instance) {
                instance = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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



    public Handler getmMainHandler() {
        return mMainHandler;
    }

    public void setmMainHandler(Handler mMainHandler) {
        this.mMainHandler = mMainHandler;
    }

    public InetSocketAddress getmInetSocketAddress() {
        return mInetSocketAddress;
    }

    public void setmInetSocketAddress(InetSocketAddress mInetSocketAddress) {
        this.mInetSocketAddress = mInetSocketAddress;
    }

    public String getmMessage() {
        return mMessage;
    }

    public void setmMessage(String mMessage) {
        this.mMessage = mMessage;
    }
}