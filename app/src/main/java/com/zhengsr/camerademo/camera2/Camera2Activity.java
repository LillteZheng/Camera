package com.zhengsr.camerademo.camera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import com.zhengsr.camerademo.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {
    private static final String TAG = "Camera2Activity";
    private String mCameraID;
    private int mFacing = CameraCharacteristics.LENS_FACING_BACK;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCharacteristics;
    private TextureView mTextureView;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mTextureView = findViewById(R.id.texture);

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);


    }



    @Override
    protected void onResume() {
        super.onResume();


    }



    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

    /**
     * 开启相机
     */
    @SuppressLint("MissingPermission")
    private void openCamera(int width,int height) {
        setupPreviewSize(width,height);
        try {
            mCameraManager.openCamera(mCameraID,mCameraDeviceCallback,null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mCameraID, e);
        }
    }

    private void setupPreviewSize(int width, int height) {
        StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        Size largest = Collections.max(
                Arrays.asList(sizes),
                new CompareSizesByArea());
        //设置imagereader
        mImageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),
                ImageFormat.JPEG,2);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //返回来的数据
                Log.d(TAG, "zsr onImageAvailable: ");
            }
        },null);
    }


    private void closeCamera(){
        if (mCameraDevice != null){
            mCameraDevice.close();
        }
    }




    /**
     * 检查camera2的相机属性
     * @return
     */
    private boolean chooseCameraIdByFacing(){
        String backid = null;
        String frontid = null;

        try {
            String[] idList = mCameraManager.getCameraIdList();
            if (idList.length == 0){
                throw new RuntimeException("No camera here! ");
            }
            for (String id : idList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                //当不满足 camera2 的功能时，退出
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                    continue;
                }
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null){
                    throw new RuntimeException("Cannot find camera facing");
                }
                if (facing == CameraCharacteristics.LENS_FACING_BACK){
                    backid = id;
                }else if (facing == CameraCharacteristics.LENS_FACING_FRONT){
                    frontid = id;
                }

            }

            //默认后置摄像头
            if (TextUtils.isEmpty(backid)){
                mFacing = CameraCharacteristics.LENS_FACING_BACK;
                mCameraID = backid;
                mCharacteristics = mCameraManager.getCameraCharacteristics(backid);
                return true;
            }
            //没有则选择前置摄像头
            if (TextUtils.isEmpty(frontid)){
                mFacing = CameraCharacteristics.LENS_FACING_FRONT;
                mCameraID = frontid;
                mCharacteristics = mCameraManager.getCameraCharacteristics(frontid);
                return true;
            }
            //若还是没有，则就是外置摄像头，则选择后置的即可
            mFacing = CameraCharacteristics.LENS_FACING_BACK;
            mCameraID = backid;
            mCharacteristics = mCameraManager.getCameraCharacteristics(backid);
            return true;


        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;

    }

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                //camera2 是一个会话模式，如果要开预览，
                CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
               // captureRequest.addTarget(mTextureView.getHolder().getSurface());
               // mCameraDevice.createCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
           // mCallback.onCameraClosed();
            mCameraDevice.close();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCameraDevice = null;
        }

    };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture
                , int width, int height) {
            // 当TextureView可用时，打开摄像头
            openCamera(width, height);
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture
                , int width, int height){ }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) { return true; }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture){}
    };

    public void switchC(View view) {
    }

    public void tackPicture(View view) {
    }

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
    private Size getPreviewSize(int width,int height){
        StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(TextureView.class);
        for (Size size : sizes) {
            
        }

    }
}
