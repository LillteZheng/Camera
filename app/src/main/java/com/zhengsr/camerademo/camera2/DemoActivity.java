package com.zhengsr.camerademo.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.zhengsr.camerademo.CameraUtils;
import com.zhengsr.camerademo.R;
import com.zhengsr.camerademo.RecordManager;
import com.zhengsr.camerademo.view.AutoFitTextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by smile on 2019/5/27.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DemoActivity extends AppCompatActivity {

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private static final String TAG = "DemoActivity";
    private AutoFitTextureView mTextureView;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //优先使用前置摄像头
    private String mCameraId ;
    private ImageReader mImageReader;
    private Size mPreviewSize;
    private CameraManager mCamreaManager;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private MediaRecorder mMediaRecorder;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Integer mSensorOrientation;
    private Size mVideoSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_layout);
        mTextureView = findViewById(R.id.texture);

    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        //当camera退回来，且textureview 是可用的
        if (mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        }else{ // 刚启动
            mTextureView.setSurfaceTextureListener(mTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void closeCamera(){
        try {
            if (null != mCaptureSession) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启一个 HandlerThread
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                
            }
        };
    }

    /**
     * 关闭HandlerThread线程
     */
    private void stopBackgroundThread(){
        if (mBackgroundThread != null){
            mBackgroundThread.quitSafely();
        }
        try {
            //等待退出结束
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 监听 TextureView 是否可用
     */
    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //在这里开启相机
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width,height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        //设置预设大小
        setUpCameraOutputs(width, height);
        configureTransform(width,height);
        try {
            mCamreaManager.openCamera(mCameraId,mCameraCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setUpCameraOutputs(int width, int height) {
        try {
            mCamreaManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] idList = mCamreaManager.getCameraIdList();
            for (String id : idList) {
                //拿到 CameraCharacteristics
                CameraCharacteristics characteristics = mCamreaManager.getCameraCharacteristics(id);

                //判断level
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                //这个表示和 camera1 一样，并不支持 camera2 的一些属性，其实可以去掉
                /*if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                    continue;
                }*/
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //优先使用前置摄像头
                //CameraCharacteristics.LENS_FACING_FRONT
                Log.d(TAG, "zsr setUpCameraOutputs: "+facing);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    mCameraId = id;
                    break;
                }
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    mCameraId = id;
                 //   break;
                }
                mCameraId = id;
            }
            Log.d(TAG, "zsr mCameraId: "+mCameraId);
            CameraCharacteristics characteristics = mCamreaManager.getCameraCharacteristics(mCameraId);
            //获取 iamgereader
            mImageReader =
                    ImageReader.newInstance(width,height, ImageFormat.YUV_420_888,2);
            //监听是否有图片生成
            mImageReader.setOnImageAvailableListener(mImageAvailableListener,mBackgroundHandler);
            //判断是否要根据摄像头的位置去调整预览的大小属性
            int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean isNeedSwap = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        isNeedSwap = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        isNeedSwap = true;
                    }
                    break;
                default:
                    Log.e(TAG, "zsr Display rotation is invalid: " + displayRotation);
            }
            Point point = new Point();
            getWindowManager().getDefaultDisplay().getSize(point);
            //旋转的角度
            int rotationWidth = width;
            int rotationHeight = height;
            //最大支持的预览大小为屏幕大小
            int maxPreviewWidth = point.x;
            int maxPreviewHeight = point.y;
            //如果需要调整
            if (isNeedSwap){
                //因为摄像头的是长宽这样设置的，所以需要翻转一下
                rotationWidth = height;
                rotationHeight = width;
                maxPreviewWidth = point.y;
                maxPreviewHeight = point.x;
            }


            //设置预览大小
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //图片支持的最大大小
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mMediaRecorder = new MediaRecorder();
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),rotationWidth,
                    rotationHeight,maxPreviewWidth,maxPreviewHeight,largest);
            Log.d(TAG, "zsr setUpCameraOutputs: "+ mPreviewSize.getWidth()+" "+ mPreviewSize.getHeight());

            int orientation = getResources().getConfiguration().orientation;
            Log.d(TAG, "zsr 方向: "+displayRotation+" "+orientation);
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 找到一个最适合相机的值
     * @param choices
     * @param textureViewWidth
     * @param textureViewHeight
     * @param maxWidth
     * @param maxHeight
     * @param aspectRatio
     * @return
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth,
                                          int maxHeight, Size aspectRatio){

        //收集比预览 surface 大的分辨率
        List<Size> bigEnough = new ArrayList<>();
        //收集比预览 surface 小的分辨率
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        Log.d(TAG, "zsr UI；预览大小: "+textureViewWidth+" "+textureViewHeight);
        Log.d(TAG, "zsr 最大尺寸: "+maxWidth+" "+maxHeight);
        Log.d(TAG, "zsr 图片大小: "+w+" "+h);
        float maxradio = h/w;
        for (Size option : choices) {
          //  Log.d(TAG, "zsr chooseOptimalSize: "+option.toString());
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        //优先拿到和相机匹配的分辨率，过大可能导致崩溃问题
       // return  new Size(648,480);
        if (bigEnough.size() > 0 ){
            return Collections.min(bigEnough,new CompareSizesByArea());
        }else if (notBigEnough.size() > 0){
            return Collections.max(notBigEnough,new CompareSizesByArea());
        }else{
            return choices[0];
        }

    }


    /**
     * 对比，拿到最大的数值
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * 处理图片预览倒置
     * @param viewWidth
     * @param viewHeight
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize ) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

        private CameraDevice mCameraDevice;
    CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {


        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //相机已经打开，可以开始预览了
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;


        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    /**
     * 创建session
     */
    private void createCameraPreviewSession(){
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            Log.d(TAG, "zsr createCameraPreviewSession: "+mTextureView.getWidth()+" "+mTextureView.getHeight()+" "+mPreviewSize.toString());
            //需要输出的 surface
            texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            //创建预览的 request
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //把surface导进去
            mCaptureRequestBuilder.addTarget(surface);

            //为预览创建一个 CameraCaptureSession
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCaptureSession = session;
                        //设置自动聚焦模式
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //设置自动开启flash
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        mCaptureRequest = mCaptureRequestBuilder.build();

                        mCaptureSession.setRepeatingRequest(mCaptureRequest,null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(DemoActivity.this, "配置失败!", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePic(View view) {
        try {
            ImageView imageView = findViewById(R.id.hehe);
            //使用这种方式能快速拍照；因为  JPG 格式大，容易造成卡顿
            Bitmap bitmap = mTextureView.getBitmap();
            imageView.setImageBitmap(bitmap);
            //创建一个拍照的buildr
            CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //把拍照的surface存进来
            captureRequest.addTarget(mImageReader.getSurface());

            captureRequest.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequest.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            Log.d(TAG, "zsr takePic: "+rotation);
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION
                    , getOrientation(rotation));
            // 停止连续取景
            mCaptureSession.stopRepeating();
            Log.d(TAG, "zsr takePic: 开始拍照");
            mCaptureSession.capture(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.d(TAG, "zsr onCaptureCompleted: 拍照完成");
                    try {
                        //设置自动聚焦模式
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //设置自动开启flash
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        mCaptureRequest = mCaptureRequestBuilder.build();

                        mCaptureSession.setRepeatingRequest(mCaptureRequest,null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            },mBackgroundHandler);


        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "zsr 拍照失败: "+e.toString());
        }
    }

    ImageReader.OnImageAvailableListener mImageAvailableListener  = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try {
                Log.d(TAG, "zsr 图片了");
                Image image = reader.acquireLatestImage();
                File file
                        = new File(Environment.getExternalStorageDirectory().getPath(),"camera2.png");
                mBackgroundHandler.post(new ImageSaver(image,file));
                Toast.makeText(DemoActivity.this, "生成图片: "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.d(TAG, "zsr onerror: "+e.toString());
                e.printStackTrace();
            }

        }
    };

    public void startRecord(View view) {

        try {
            closePreviewSession();
            File file = new File(Environment.getExternalStorageDirectory().getPath(),"test.mp4");
            setupMediaRecorder(file.getAbsolutePath(),mVideoSize.getWidth(),
                    mVideoSize.getHeight(),
                    mSensorOrientation);

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mCaptureRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mCaptureRequestBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(DemoActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    public void stopRecord(View view) {
        mMediaRecorder.stop();
        mMediaRecorder.release();
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }


    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void setupMediaRecorder(String path,int videoWidth,int videoHeight,int sensorOrientation){
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mMediaRecorder.setOutputFile(path);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(videoWidth, videoHeight);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (sensorOrientation) {
                case 90:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case 270:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
