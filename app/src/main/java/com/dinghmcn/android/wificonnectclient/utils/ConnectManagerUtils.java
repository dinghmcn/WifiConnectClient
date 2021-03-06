package com.dinghmcn.android.wificonnectclient.utils;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The type Connect manager.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class ConnectManagerUtils {
  /**
   * The constant CONNECT_FAILED.
   */
  public static final int CONNECT_FAILED = -1;
  /**
   * The constant CONNECT_CLOSED.
   */
  public static final int CONNECT_CLOSED = 0;
  /**
   * The constant CONNECT_SUCCESS.
   */
  public static final int CONNECT_SUCCESS = 1;
  /**
   * The constant COMMAND_RECEIVE.
   */
  public static final int COMMAND_RECEIVE = 2;
  /**
   * The constant COMMAND_SEND.
   */
  public static final int COMMAND_SEND = 3;
  /**
   * The constant COMMAND_ERROR.
   */
  public static final int COMMAND_ERROR = 4;
  private static final String TAG = ConnectManagerUtils.class.getSimpleName();
  /**
   * The constant mConnected.
   */
  public static boolean mConnected = false;
  @Nullable
  private static ConnectManagerUtils instance = null;
  @Nullable
  private Handler mMainHandler;

  private InetSocketAddress mInetSocketAddress;
  private Socket mSocket;
  @Nullable
  private ExecutorService mThreadPool;

  private InputStream is;
  private OutputStream out;

  private ConnectManagerUtils(Handler handler, InetSocketAddress inetSocketAddress) {
    mMainHandler = handler;
    mInetSocketAddress = inetSocketAddress;

    mThreadPool = new ThreadPoolExecutor(5, 9, 5, TimeUnit.SECONDS,
        new LinkedBlockingDeque<>(), r -> new Thread(r, "ConnectManagerUtils"));
  }

  /**
   * New instance connect manager.
   *
   * @param handler           the handler
   * @param inetSocketAddress the inet socket address
   * @return the connect manager
   */
  @Nullable
  public static ConnectManagerUtils newInstance(Handler handler,
      InetSocketAddress inetSocketAddress) {
    if (null == instance) {
      instance = new ConnectManagerUtils(handler, inetSocketAddress);
    }
    return instance;
  }


  /**
   * 判断IP地址的合法性，这里采用了正则表达式的方法来判断
   * return true，合法.
   *
   * @param ipAddress ip address
   * @return boolean boolean
   */
  public static boolean isIp(@Nullable String ipAddress) {
    if (null != ipAddress && !ipAddress.isEmpty()) {
      // 定义正则表达式
      String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
          + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
          + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
          + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
      // 判断ip地址是否与正则表达式匹配
      // 返回判断信息
      // 返回判断信息
      return ipAddress.matches(regex);
    }
    return false;
  }

  /**
   * Is integer boolean.
   *
   * @param str the str
   * @return the boolean
   */
  public static boolean isInteger(@Nullable String str) {
    if (null == str || str.isEmpty()) {
      return false;
    }
    String regex = "^[-+]?[\\d]*$";
    return str.matches(regex);
  }

  private static String getFileMd5(File file) {
    try {
      FileInputStream fileInputStream = new FileInputStream(file);
      byte[] buffer = new byte[8192];
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      int length;
      while ((length = fileInputStream.read(buffer)) != -1) {
        messageDigest.update(buffer, 0, length);
      }
      return new BigInteger(1, messageDigest.digest()).toString(16);
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Receive message from server.
   */
  private void receiveMessageFromServer() {
    Log.d(TAG, "Receive message.");
    mThreadPool.execute(() -> {
      sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_RECEIVE);
      int commandNullCount = 0;
      while (mConnected && !mSocket.isClosed() && mSocket.isConnected()
          && !mSocket.isInputShutdown()) {
        Log.d(TAG, "Start receive message.");
        try {
          is = mSocket.getInputStream();
          String command;
          byte[] tempBuffer = new byte[2048];
          int numReadedBytes = is.read(tempBuffer, 0, tempBuffer.length);
          if (numReadedBytes > 0) {
            command = new String(tempBuffer, 0, numReadedBytes);
          } else {
            ++commandNullCount;
            Log.d(TAG, "Command is null:" + commandNullCount);
            if (commandNullCount > 3) {
              sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_CLOSED);
              return;
            } else {
              continue;
            }
          }
          Log.d(TAG, "Command:" + command);

          if (!command.isEmpty()) {
            parsingCommand(command);
            sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_SEND, command);
          } else {
            sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_ERROR);
          }
        } catch (IOException e) {
          Log.d(TAG, "Receive message error.");
          e.printStackTrace();
        }
      }
      if (null != mMainHandler) {
        sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_CLOSED);
      }

    });
  }

  /**
   * Connect server.
   *
   * @param wifiManagerUtils the wifi manager utils
   * @param wifissid         the wifissid
   * @param wifiPassWord     the wifi pass word
   */
  public void connectServer(@NonNull final WifiManagerUtils wifiManagerUtils,
      @NonNull final String wifissid,
      final String wifiPassWord) {
    Log.d(TAG, "Connect server.");
    assert mThreadPool != null;
    mThreadPool.execute(() -> {
      long delayedTime = 8000L;
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() < start + delayedTime) {
        if (wifiManagerUtils.isWifiEnabled()) {
          if (!wifiManagerUtils.isWifiConnected(wifissid)) {
            wifiManagerUtils.connectWifi(wifissid, wifiPassWord);
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          } else {
            break;
          }
        } else {
          wifiManagerUtils.openWifi();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }

      if (wifiManagerUtils.isWifiConnected(wifissid)) {
        Log.e(TAG, "Connect Wifi success!");
      } else {
        Log.e(TAG, "Connect Wifi failed!");
      }

      start = System.currentTimeMillis();
      while (System.currentTimeMillis() < start + delayedTime) {
        Process process = null;
        try {
          process = Runtime.getRuntime()
              .exec(
                  "/system/bin/ping -c 1 -w 100 "
                      + mInetSocketAddress.getHostName());
        } catch (IOException e) {
          e.printStackTrace();
        }
        int status;
        try {
          status = process != null ? process.waitFor() : 0;
          if (status == 0) {
            break;
          }
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }


      mSocket = new Socket();
      try {
        mSocket.connect(mInetSocketAddress, 3000);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.d(TAG, mInetSocketAddress + "Connected :" + mSocket.isConnected());
      if (mSocket.isConnected()) {
        mConnected = true;
        receiveMessageFromServer();
        sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_SUCCESS);
      } else {
        sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_FAILED);
      }

    });
  }

  /**
   * Send file to server.
   *
   * @param fileUri the file uri
   */
  public void sendFileToServer(@NonNull final Uri fileUri) {
    Log.d(TAG, "Send File :" + fileUri);
    assert mThreadPool != null;
    mThreadPool.execute(() -> {
      try {
        Log.d(TAG, "" + !mConnected + mSocket.isClosed() + !mSocket.isConnected()
            + mSocket.isOutputShutdown());
        if (!mConnected || mSocket.isClosed() || !mSocket.isConnected()
            || mSocket.isOutputShutdown()) {
          sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_CLOSED);
          return;
        }

        File file = new File(fileUri.getPath());
        out = mSocket.getOutputStream();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("FileName", file.getName());
        jsonObject.put("FileSize", file.length());
        jsonObject.put("FileMd5", getFileMd5(file));

        sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_SEND, jsonObject.toString());
        Log.d(TAG, jsonObject.toString());

        out.write(jsonObject.toString().getBytes("UTF-8"));
        out.flush();

        FileInputStream fileInputStream = new FileInputStream(file);
        int size;
        byte[] buffer = new byte[8192];
        while ((size = fileInputStream.read(buffer)) != -1) {
          out.write(buffer, 0, size);
        }
        out.flush();
        fileInputStream.close();
      } catch (JSONException | IOException e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * Disconnect server.
   */
  public void disconnectServer() {
    Log.d(TAG, "Disconnect server.");
    try {
      if (null != is) {
        is.close();
      }
      if (null != out) {
        out.close();
      }
      if (null != mSocket) {
        mSocket.close();
        mConnected = false;
        sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_CLOSED);
      }
      if (null != instance) {
        instance = null;
      }
      if (null != mMainHandler) {
        mMainHandler = null;
      }
      if (null != mThreadPool) {
        mThreadPool.shutdownNow();
        mThreadPool = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Send message to server.
   *
   * @param message the message
   */
  public void sendMessageToServer(final String message) {
    Log.d(TAG, "Send message :" + message);
    sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_SEND, message);
    assert mThreadPool != null;
    mThreadPool.execute(() -> {
      try {
        Log.d(TAG, "" + !mConnected + mSocket.isClosed() + !mSocket.isConnected()
            + mSocket.isOutputShutdown());
        if (!mConnected || mSocket.isClosed() || !mSocket.isConnected()
            || mSocket.isOutputShutdown()) {
          sendMessage(EnumCommand.CONNECT.ordinal(), CONNECT_CLOSED);
          return;
        }
        out = mSocket.getOutputStream();
        out.write(message.getBytes("UTF-8"));
        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private void sendMessage(int what, int arg1) {
    sendMessage(what, arg1, null);
  }

  private void parsingCommand(String commands) {
    Log.d(TAG, commands);
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(commands);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    String command = null;
    if (jsonObject != null) {
      command = jsonObject.optString("Command", "Error");
    }
    if (null != command && !command.isEmpty()) {
      sendMessage(EnumCommand.valueOf(command.toUpperCase())
          .ordinal(), -1, jsonObject);
    } else {
      sendMessage(EnumCommand.COMMAND.ordinal(), COMMAND_ERROR);
    }
  }

  /**
   * Gets main handler.
   *
   * @return the main handler
   */
  @Nullable
  public Handler getmMainHandler() {
    return mMainHandler;
  }

  /**
   * Sets main handler.
   *
   * @param mMainHandler the m main handler
   */
  public void setmMainHandler(Handler mMainHandler) {
    this.mMainHandler = mMainHandler;
  }

  /**
   * Gets inet socket address.
   *
   * @return the inet socket address
   */
  public InetSocketAddress getmInetSocketAddress() {
    return mInetSocketAddress;
  }

  /**
   * Sets inet socket address.
   *
   * @param mInetSocketAddress the  inetsocket address
   */
  public void setmInetSocketAddress(InetSocketAddress mInetSocketAddress) {
    this.mInetSocketAddress = mInetSocketAddress;
  }

  /**
   * The enum Enum command.
   */
  public enum EnumCommand {
    /**
     * Connect enum command.
     */
    CONNECT,
    /**
     * Command enum command.
     */
    COMMAND,
    /**
     * Sensor enum command.
     */
    SENSOR,
    /**
     * Camera enum command.
     */
    CAMERA,
    /**
     * Function enum command.
     */
    FUNCTION,
    /**
     * Show picture enum command.
     */
    SHOW_PICTURE
  }

  private void sendMessage(int what, int arg1, Object obj) {
    Message message = Message.obtain();
    message.what = what;
    message.arg1 = arg1;
    message.obj = obj;
    assert mMainHandler != null;
    mMainHandler.sendMessage(message);
  }
}
