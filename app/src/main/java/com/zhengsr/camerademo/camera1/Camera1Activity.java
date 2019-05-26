package com.zhengsr.camerademo.camera1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.zhengsr.camerademo.CameraUtils;
import com.zhengsr.camerademo.R;
import com.zhengsr.camerademo.RecordManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class Camera1Activity extends AppCompatActivity {
    private static final String TAG = "Camera1Fragment";
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private int mCameraID;
    private Camera.Parameters mParameters;
    private int mFacing = 0; //默认为后置摄像头
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private boolean mPreviewShowing = false;
    private RecordManager mRecordManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(mSurfaceCall);
        findViewById(R.id.record).setOnTouchListener(RecordTouchListener);



    }


    @Override
    protected void onResume() {
        super.onResume();
        chooseCamera();
        openCamera();

    }

    @Override
    protected void onPause() {
        mPreviewShowing = false;
        stopPreview();
        releaseCamera();
        super.onPause();
    }

    /**
     * 选择摄像头
     */
    private void chooseCamera(){
        int nums = Camera.getNumberOfCameras();
        for (int i = 0; i < nums; i++) {
            Camera.getCameraInfo(i,mCameraInfo);
            if (mCameraInfo.facing == mFacing){
                mCameraID = i;
                return;
            }
        }
    }
    private void openCamera(){
        //如果camera已经启动，先释放
        if (mCamera != null){
            releaseCamera();
        }
        mCamera = Camera.open(mCameraID);
        mParameters = mCamera.getParameters();

        //设置自动聚焦
        // mParameters.set(Camera.Parameters.FOCUS_MODE_AUTO,);
        List<String> modes = mParameters.getSupportedFocusModes();
        //查看支持的聚焦模式
        for (String mode : modes) {
            //默认图片聚焦模式
            if (mode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                break;
            }
        }

        //闪光灯
        mCamera.setParameters(mParameters);

        //矫正方向
        mCamera.setDisplayOrientation(CameraUtils.getCameraDisplayOrientation(mCameraInfo,this));
    }

    /**
     * 释放camera，避免占用
     */
    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }


    private void setupPreview(){
        try {
            if (mPreviewShowing){
                stopPreview();
            }
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview(){
        if (mCamera != null){
            mCamera.stopPreview();
        }
    }
    private int mShortSize,mLongSize;
    SurfaceHolder.Callback mSurfaceCall  = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "zsr surfaceChanged: "+width+" "+height);
            mShortSize = width;
            mLongSize = height;
            if (isSurfaceReady()) {
                rejustSize();
                setupPreview();
            }

            mPreviewShowing = true;
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"record.mp4");
            if (file.exists()){
                file.delete();
            }
            mRecordManager = new RecordManager(mCamera,mSurfaceView.getHolder(),file.getAbsolutePath());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    /**
     * 调整预览大小
     */
    private void rejustSize() {
        if (mLongSize != 0 && mShortSize != 0) {
            float offsert = mLongSize * 1.0f / mShortSize;
            List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
            for (Camera.Size size : sizes) {
                float asradio = size.width * 1.0f / size.height;
                if (offsert == asradio && size.height <= mShortSize && size.width <= mLongSize) {
                    //设置这个要根据横竖屏来，且需要预览开启之前，这样拍出来的图片比较清晰。demo用竖屏来了
                    Log.d(TAG, "zsr 最终: "+size.width+" "+size.height+" "+mCameraInfo.orientation);
                    mParameters.setPreviewSize(size.width,size.height);
                    mParameters.setPictureSize(size.width,size.height);
                    mCamera.setParameters(mParameters);
                    break;
                }
            }
        }

    }

    private boolean isSurfaceReady(){
        return mSurfaceView != null && mSurfaceView.getWidth() != 0 ;
    }

    public void switchC(View view) {
        if (mFacing == Camera.CameraInfo.CAMERA_FACING_BACK){
            mFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else{
            mFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        releaseCamera();
        chooseCamera();
        openCamera();
        if (isSurfaceReady()){
            rejustSize();
            setupPreview();
        }
    }

    public void tackPicture(View view) {
        if (mCamera != null){
            mCamera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                    //在拍照的瞬间被回调

                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //返回未经压缩的图像数据
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //返回postview类型的图像数据,这里可以对图片进行存储
                    try {
                        File file = new File(Environment.getExternalStorageDirectory().getPath(),"test.png");
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(data);
                        fos.close();
                        MediaScannerConnection.scanFile(Camera1Activity.this, new String[]{file.getAbsolutePath()}, null, null);
                        //部分手机存在拍照旋转问题
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        Toast.makeText(Camera1Activity.this, "图片已经存储: "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

                        //通知更新图库
                        reJustPictureDegrees(file.getAbsolutePath(),bitmap);
                      //  MediaScannerConnection.scanFile(Camera1Activity.this, new String[]{file.getAbsolutePath()}, null, null);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"+file.getAbsolutePath())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }
    private void reJustPictureDegrees(String path,Bitmap bitmap){
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int degrees = 0;
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d(TAG, "zsr reJustPictureDegrees: "+orientation);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            // 创建新的图片
            Bitmap reBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //重新保存
            if (reBitmap != bitmap) {
                bitmap.recycle();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                reBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] datas = baos.toByteArray();

                FileOutputStream fos = new FileOutputStream(path);
                fos.write(datas);
                fos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    View.OnTouchListener RecordTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          ///  Log.d(TAG, "zsr onTouch: ");
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN){
               mRecordManager.start();
                Toast.makeText(Camera1Activity.this, "开始录制", Toast.LENGTH_SHORT).show();
            }else if (action == MotionEvent.ACTION_UP){
                mRecordManager.stop();
                Toast.makeText(Camera1Activity.this,"录制成功："+ mRecordManager.getPath(), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    };

}
