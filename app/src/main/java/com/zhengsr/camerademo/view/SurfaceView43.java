package com.zhengsr.camerademo.view;

import android.content.Context;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class SurfaceView43 extends SurfaceView {

    public SurfaceView43(Context context) {
        this(context,null);
    }

    public SurfaceView43(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SurfaceView43(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width * 4 /3;
        setMeasuredDimension(width,height);
    }
}
