package com.dinghmcn.android.wificonnectclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

public class ShowPictureFullActivity extends AppCompatActivity {
    private static final String TAG = "ShowPictureFullActivity";

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_show_picture);

        mImageView = findViewById(R.id.imageView);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
