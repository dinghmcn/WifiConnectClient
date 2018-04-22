package com.dinghmcn.android.wificonnectclient;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class CameraActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "CameraActivity";

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

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
                    String pictureName = "picture"+ mCameraView.getFacing() + ".jpg";
                    File file = new File(getExternalCacheDir(), pictureName);
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        Log.w(TAG, "Picture save to " + file);
                        os.close();
                        setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
                        finish();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                        setResult(RESULT_CANCELED);
                        finish();
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
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
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

        int cameraId = getIntent().getIntExtra("camera_id", CameraView.FACING_BACK);
        Log.d(TAG, "cameraId : " + cameraId);
        mCameraView.setFacing(cameraId);
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
}
