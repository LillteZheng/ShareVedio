package hht.com.sharevideo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by smile on 2019/5/29.
 */

public class AutoFitTextureView extends TextureView{
    private int mWidth,mHeight;
    public AutoFitTextureView(Context context) {
        this(context,null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(int width,int height){
        mWidth = width;
        mHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mWidth != 0 && mHeight != 0){
            if (mWidth*height == mHeight*width){
                setMeasuredDimension(width,width * mHeight / mWidth);
            }else{
                setMeasuredDimension(width * mWidth / mHeight,height);
            }

        }
    }
}
