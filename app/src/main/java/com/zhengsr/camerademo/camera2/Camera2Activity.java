package com.zhengsr.camerademo.camera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zhengsr.camerademo.Demo;
import com.zhengsr.camerademo.R;
import com.zhengsr.camerademo.view.AutoFitTextureView;
import com.zhengsr.camerademo.view.TextureView43;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {
    private static final String TAG = "Camera2Activity";
    private String mCameraID;
    private int mFacing = CameraCharacteristics.LENS_FACING_BACK;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCharacteristics;
    private AutoFitTextureView mTextureView;
    private ImageReader mImageReader;
    private CameraCaptureSession mCaptureSession;

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
        chooseCameraIdByFacing();
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
                ImageFormat.JPEG,2); //最大连拍为2

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //当有图片数据时，回调该接口
                Log.d(TAG, "zsr onImageAvailable: ");
                // 获取捕获的照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                // 使用IO流将照片写入指定文件
                File file = new File(getExternalFilesDir(null), "pic.jpg");
                buffer.get(bytes);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    output.write(bytes);
                    Toast.makeText(Camera2Activity.this, "保存: "
                            + file, Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    image.close();
                }
            }
        },null);

        //设置最佳预览尺寸
        Size previewSize = chooseOptimalSize(map.getOutputSizes(
                SurfaceTexture.class),width,height,largest);
        Log.d(TAG, "zsr setupPreviewSize: "+previewSize.getWidth()+" "+previewSize.getHeight());
        int orientation = getResources().getConfiguration().orientation;
        if (previewSize != null){
            //横屏
            if (orientation == Configuration.ORIENTATION_LANDSCAPE){
                mTextureView.setAspectRatio(previewSize.getWidth(),previewSize.getHeight());
            }else{
                mTextureView.setAspectRatio(previewSize.getHeight(),previewSize.getWidth());
            }
        }

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
                /*if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                    continue;
                }*/
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
            if (!TextUtils.isEmpty(backid)){
                mFacing = CameraCharacteristics.LENS_FACING_BACK;
                mCameraID = backid;
                mCharacteristics = mCameraManager.getCameraCharacteristics(backid);
                return true;
            }
            //没有则选择前置摄像头
            if (!TextUtils.isEmpty(frontid)){
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
            //摄像头已经可以打开，开始预览
            mCameraDevice = camera;
            createCameraPreviewSession();

            Log.d(TAG, "zsr onOpened: ");
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
           // mCallback.onCameraClosed();
            mCameraDevice.close();
            mCameraDevice = null;
            Log.d(TAG, "zsr onClosed: ");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCameraDevice.close();
            mCameraDevice = null;
            Log.d(TAG, "zsr onError: ");
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

    private CaptureRequest mCaptureBuilder;
    private void createCameraPreviewSession(){
        try {
            //设置预览大小
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mTextureView.getWidth(),mTextureView.getHeight());
            // 创建作为预览的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将textureView的surface作为CaptureRequest.Builder的目标
            Surface surface = new Surface(surfaceTexture);
            captureRequest.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {



                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        //如果配置成功,则开始配置相机的一些属性
                        mCaptureSession = session;
                        //设置图片自动聚焦
                        captureRequest.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //设置自动曝光
                        captureRequest.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        //开始预览
                        mCaptureBuilder = captureRequest.build();

                        // 设置预览时连续捕获图像数据
                        mCaptureSession.setRepeatingRequest(mCaptureBuilder,null,null);
                    } catch (CameraAccessException e) {
                        Log.d(TAG, "zsr error : "+e.toString());
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(Camera2Activity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

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
    private Size getPreviewSize(int shortSize,int longSize){
        StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
        float radio = longSize * 1.0f / shortSize;
        Log.d(TAG, "zsr getPreviewSize: "+shortSize+" "+longSize+" "+radio);
        for (Size size : sizes) {
            float asradio = size.getWidth() * 1.0f / size.getHeight();
            if (radio == asradio && size.getHeight() <= shortSize && size.getWidth() <= longSize) {
                //设置这个要根据横竖屏来，且需要预览开启之前，这样拍出来的图片比较清晰。demo用竖屏来了
                Log.d(TAG, "zsr 最终: "+size.getWidth()+" "+size.getHeight()+" ");

                return size;
            }
        }
        return null;

    }
    private  Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio) {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            System.out.println("找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }
}
