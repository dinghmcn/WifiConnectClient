package com.dinghmcn.android.wificonnectclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * The type Show picture full activity.
 *
 * @author dinghmcn
 * @date 2018 /4/20 10:47
 */
public class ShowPictureFullActivity extends AppCompatActivity {
  private static final String TAG = "ShowPictureFullActivity";

  private ImageView mImageView;

  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Window window = getWindow();
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.activity_full_show_picture);

    mImageView = findViewById(R.id.imageView);
  }

  /**
   * On resume.
   */
  @Override
  protected void onResume() {
    super.onResume();

    int resId = getIntent().getIntExtra("res_id", -1);
    if (resId > 0) {
      mImageView.setImageResource(resId);
    } else {
      finish();
    }
  }

  /**
   * On pause.
   */
  @Override
  protected void onPause() {
    super.onPause();
    finish();
  }

  /**
   * On back pressed.
   */
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
  }
}
