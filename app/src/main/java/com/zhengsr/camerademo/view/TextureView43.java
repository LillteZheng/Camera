package com.zhengsr.camerademo.view;

import android.content.Context;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by smile on 2019/5/27.
 */

public class TextureView43 extends TextureView {

    private int mWidth = 0;
    private int mHeight = 0;

    public TextureView43(Context context) {
        this(context,null);
    }

    public TextureView43(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TextureView43(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void resetSize(int width,int height){
        mWidth = width;
        mHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mWidth || 0 == mHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mWidth / mHeight) {
                setMeasuredDimension(width, width * mHeight / mWidth);
            } else {
                setMeasuredDimension(height * mWidth / mHeight, height);
            }
        }

    }
}
