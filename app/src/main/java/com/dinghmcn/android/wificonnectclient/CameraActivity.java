package com.dinghmcn.android.wificonnectclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The type Camera activity.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
public class CameraActivity extends AppCompatActivity implements
    ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String TAG = "CameraActivity";

  @Nullable
  private CameraView mCameraView;

  @Nullable
  private Handler mBackgroundHandler;

  private float mCompressionRatio = 1;
  private int mPictureWidth = -1;
  private int mPictureHeight = -1;

  @Nullable
  private CameraView.Callback mCallback = new CameraView.Callback() {

    @Override
    public void onCameraOpened(CameraView cameraView) {
      Log.d(TAG, "onCameraOpened");
    }

    @Override
    public void onCameraClosed(CameraView cameraView) {
      Log.d(TAG, "onCameraClosed");
    }

    @Override
    public void onPictureTaken(CameraView cameraView, final byte[] data) {
      Log.d(TAG, "onPictureTaken " + data.length);
      getBackgroundHandler().post(new Runnable() {
        @Override
        public void run() {
          compressBySize(data, mCompressionRatio);
        }
      });
    }

  };


  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    Window window = getWindow();
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_camera);

    mCameraView = findViewById(R.id.camera);

    if (null != mCameraView) {
      init();
    }
  }

  private void init() {
    mCameraView.addCallback(mCallback);
    mCameraView.start();

    String parameter = getIntent().getStringExtra("camera_parameter");
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(parameter);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    int cameraId = jsonObject.optInt("CameraId", CameraView.FACING_BACK);
    Log.d(TAG, "cameraId : " + cameraId);
    mCameraView.setFacing(cameraId);

    String resolutionRatio = jsonObject.optString("ResolutionRatio", "");
    if (!resolutionRatio.isEmpty()) {
      setPictureSize(resolutionRatio);
    }

    boolean isFocus = jsonObject.optBoolean("IsFocus", true);
    mCameraView.setAutoFocus(isFocus);

    int flash = jsonObject.optInt("FlashMode", 3);
    mCameraView.setFlash(flash);

    mCompressionRatio = Double.valueOf(jsonObject.optDouble("CompressionRatio", 1.0))
        .floatValue();
    Log.d(TAG, "ratio:" + mCameraView.getAspectRatio()
        .toString());

    getBackgroundHandler().postDelayed(new Runnable() {
      @Override
      public void run() {
        mCameraView.takePicture();
      }
    }, 1000);
  }

  /**
   * On pause.
   */
  @Override
  protected void onPause() {
    mCameraView.stop();
    super.onPause();
    Log.d(TAG, "onPause()");
    finish();
  }

  /**
   * On destroy.
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mBackgroundHandler != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        mBackgroundHandler.getLooper()
            .quitSafely();
      } else {
        mBackgroundHandler.getLooper()
            .quit();
      }
      mBackgroundHandler = null;
    }

    if (null != mCameraView) {
      mCameraView = null;
    }

    if (null != mCallback) {
      mCallback = null;
    }
  }


  @Nullable
  private Handler getBackgroundHandler() {
    if (mBackgroundHandler == null) {
      HandlerThread thread = new HandlerThread("background");
      thread.start();
      mBackgroundHandler = new Handler(thread.getLooper());
    }
    return mBackgroundHandler;
  }

  /**
   * On back pressed.
   */
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
  }

  private void setPictureSize(String s) {
    int position = s.indexOf(':');
    if (position == -1) {
      throw new IllegalArgumentException("Malformed aspect ratio: " + s);
    }
    try {
      mPictureWidth = Integer.parseInt(s.substring(0, position));
      mPictureHeight = Integer.parseInt(s.substring(position + 1));
      float ratio = 1F * mPictureWidth / mPictureHeight;
      mCameraView.setAspectRatio(AspectRatio.of(mPictureWidth, mPictureHeight));
      Log.d(TAG, "ratio:" + ratio + "|" + mCameraView.getAspectRatio()
          .toString());
      Log.d(TAG, "ratios:" + mCameraView.getSupportedAspectRatios()
          .size());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Malformed aspect ratio: " + s, e);
    }
  }

  /**
   * Compress by size.
   *
   * @param data             the data
   * @param compressionRatio the compression ratio
   */
  private void compressBySize(@NonNull byte[] data, float compressionRatio) {

    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
    bitmap = zoomImage(bitmap, mPictureHeight, mPictureWidth, compressionRatio);

    String pictureName = "picture" + mCameraView.getFacing() + ".jpg";
    File file = new File(getExternalCacheDir(), pictureName);
    BufferedOutputStream bos = null;
    try {
      bos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
      bos.flush();
      bos.close();
      Log.w(TAG, "Picture save to " + file);
      setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
      finish();
    } catch (IOException e) {
      Log.w(TAG, "Cannot write to " + file, e);
      setResult(RESULT_CANCELED);
      finish();
    } finally {
      if (bos != null) {
        try {
          bos.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }

  private Bitmap zoomImage(Bitmap bitmap, int newWidth, int newHeight, float compressionRatio) {
    int oldWidth = bitmap.getWidth();
    int oldHeight = bitmap.getHeight();

    float scaleWidth = newWidth / oldWidth * compressionRatio;
    float scaleHeight = newHeight / oldHeight * compressionRatio;

    float scale = scaleWidth > scaleHeight ? scaleHeight : scaleWidth;
    Log.d(TAG, oldWidth + ":" + oldHeight);
    Log.d(TAG, newWidth + ":" + newHeight);
    Log.d(TAG, scale + ":" + scaleWidth + ":" + scaleHeight);

    Matrix matrix = new Matrix();
    matrix.postScale(scale, scale);

    return Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, matrix, true);
  }
}
