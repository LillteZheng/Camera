package com.zhengsr.camerademo;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class RecordManager {
    private static final String TAG = "RecordManager";
    private  MediaRecorder mMediaRecorder;
    private String mPath;
    public RecordManager(Camera camera, SurfaceHolder holder,String path) {
        //顺序很重要
        try {
            mPath = path;
            //相机必须先解锁
            camera.unlock();
            mMediaRecorder = new MediaRecorder();
            //将相机设置给MediaRecorder
            mMediaRecorder.setCamera(camera);
            // 设置录制视频源和音频源
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 设置录制的视频编码和音频编码
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoSize(1920, 1080);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoEncodingBitRate(1024*1024*20);
            mMediaRecorder.setPreviewDisplay(holder.getSurface());
            // 设置视频文件输出的路径
            mMediaRecorder.setOutputFile( path);
            mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    Log.d(TAG, "zsr onError: ");
                }
            });
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath(){
        return mPath;
    }

    public void start(){
        mMediaRecorder.start();

    }

    public void stop(){
        mMediaRecorder.stop();
        mMediaRecorder.release();
    }

    public interface  RecordListener{
        void onError();
        void start();
    }
    
}
