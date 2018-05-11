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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The type Camera activity.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
public class CameraActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String TAG = "CameraActivity";

  private CameraView mCameraView;

  private Handler mBackgroundHandler;

  private int mCompressionRatio = 1;
  private int mPictureWidth = -1;
  private int mPictureHeight = -1;

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


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    Window window = getWindow();
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_camera);

    mCameraView = findViewById(R.id.camera);

    if (null != mCameraView) {
      mCameraView.addCallback(mCallback);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    String parameter = getIntent().getStringExtra("camera_parameter");
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(parameter);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    int cameraId = jsonObject.optInt("Camera", CameraView.FACING_BACK);
    Log.d(TAG, "cameraId : " + cameraId);
    mCameraView.setFacing(cameraId);

    String resolutionRatio = jsonObject.optString("ResolutionRatio", "");
    if (!resolutionRatio.isEmpty()) {
      mCameraView.setAspectRatio(AspectRatio.parse(resolutionRatio));
    }

    boolean isFocus = jsonObject.optInt("IsFocus", 1) == 1;
    mCameraView.setAutoFocus(isFocus);

    mCompressionRatio = jsonObject.optInt("CompressionRatio", 1);

    mCameraView.start();

    getBackgroundHandler().postDelayed(new Runnable() {
      @Override
      public void run() {
        mCameraView.takePicture();
      }
    }, 1000);
  }

  @Override
  protected void onPause() {
    mCameraView.stop();
    super.onPause();
    Log.d(TAG, "onPause()");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mBackgroundHandler != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        mBackgroundHandler.getLooper().quitSafely();
      } else {
        mBackgroundHandler.getLooper().quit();
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


  private Handler getBackgroundHandler() {
    if (mBackgroundHandler == null) {
      HandlerThread thread = new HandlerThread("background");
      thread.start();
      mBackgroundHandler = new Handler(thread.getLooper());
    }
    return mBackgroundHandler;
  }

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
  private void compressBySize(byte[] data, int compressionRatio) {

    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
    bitmap = zoomImage(bitmap, mPictureWidth, mPictureHeight);

    if (compressionRatio > 1) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
      byte[] bitmapData = baos.toByteArray();

      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(bitmapData, 0, data.length, opts);
      opts.inSampleSize = compressionRatio;
      opts.inJustDecodeBounds = false;
      bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, data.length, opts);
    }

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

  private Bitmap zoomImage(Bitmap bitmap, int newWidth, int newHeight) {
    int oldWidth = bitmap.getWidth();
    int oldHeight = bitmap.getHeight();

    float scaleWidth = 1F * newWidth / oldWidth;
    float scaleHeight = 1F * newHeight / oldHeight;

    float scale = scaleWidth > scaleHeight ? scaleHeight : scaleWidth;

    Matrix matrix = new Matrix();
    matrix.postScale(scale, scale);

    return Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, matrix, true);
  }
}
