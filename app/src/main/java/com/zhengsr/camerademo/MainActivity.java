package com.zhengsr.camerademo;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.zhengsr.camerademo.camera1.Camera1Activity;
import com.zhengsr.camerademo.camera2.Camera2Activity;
import com.zhengsr.camerademo.camera2.DemoActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO},
                REQUEST_CAMERA_PERMISSION);
    }

    public void camera1(View view) {
        startActivity(new Intent(this, Camera1Activity.class));
    }

    public void camera2(View view) {
        startActivity(new Intent(this, DemoActivity.class));
    }

    public void demo(View view) {
        startActivity(new Intent(this, Demo.class));
    }
}
