package hht.com.sharevideo.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

import hht.com.sharevideo.view.AutoFitTextureView;

/**
 * Created by smile on 2019/5/29.
 */

public class Camera1 {
    private static final String TAG = "Camera1";
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private int mCameraID;
    private Camera.Parameters mParameters;
    private int mFacing = Camera.CameraInfo.CAMERA_FACING_FRONT; //默认为后置摄像头
    private Activity mActivity;
    private Camera.Size mPreviewSize;
    private PreviewListener mPreviewListener;

    private Camera1(Activity activity, PreviewListener listener) {
        mPreviewListener = listener;
        mActivity = activity;
        chooseCamera();
    }

    public static Camera1 create(Activity activity, PreviewListener listener) {
        return new Camera1(activity, listener);
    }

    public Camera1 setListener(PreviewListener listener) {
        mPreviewListener = listener;
        return this;
    }

    public void openCamera(int width, int height) {
        //若摄像头已经打开了,则先释放
        if (mCamera != null) {
            closeCamera();
        }
        mCamera = Camera.open(mCameraID);
        adjustCameraParameters(width, height);


    }


    private void adjustCameraParameters(int width, int height) {
        mParameters = mCamera.getParameters();

        //查看支持的对焦模式
        List<String> focusModes = mParameters.getSupportedFocusModes();
        for (String mode : focusModes) {
            //默认图片聚焦模式
            if (mode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                break;
            }
        }

        //闪光灯,自动模式
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        //设置预览大小
        mPreviewSize = getPreviewSize(width, height);
        Log.d(TAG, "zsr 预览大小: " + mPreviewSize.width + " " + mPreviewSize.height);
        mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        mParameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
        //矫正方向
        int displayRotation = Camera1Utils.getCameraDisplayOrientation(mCameraInfo, mActivity);
        //设置预览格式
        mParameters.setPreviewFormat(ImageFormat.YV12);
        mParameters.setRotation(displayRotation);
        mCamera.setParameters(mParameters);
        mCamera.setDisplayOrientation(displayRotation);
    }

    public void setSurface(AutoFitTextureView textureView) {
        int orientation = mActivity.getWindowManager().getDefaultDisplay().getOrientation();
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(
                    mPreviewSize.width, mPreviewSize.height);
        } else {
            textureView.setAspectRatio(
                    mPreviewSize.height, mPreviewSize.width);
        }


    }


    public void startPreview(SurfaceTexture texture) {

        try {
            stopPreview();
            mCamera.setPreviewTexture(texture);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取帧的原始数据
        final byte[] buffer = new byte[((mPreviewSize.width * mPreviewSize.height)
                * 3 / 2)];
        mCamera.addCallbackBuffer(buffer);
        //增加复用率
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mPreviewListener != null) {
                    mPreviewListener.onPreviewFrame(data);
                }
                mCamera.addCallbackBuffer(data);
            }
        });


    }


    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * 选择摄像头
     */
    private void chooseCamera() {
        int nums = Camera.getNumberOfCameras();
        for (int i = 0; i < nums; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraID = i;
                return;
            }
        }
    }

    public void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 调整预览大小
     */
    private Camera.Size getPreviewSize(int width, int height) {
        int previeWidth = width;
        int previewHeight = height;
        int orientation = mActivity.getResources().getConfiguration().orientation;
        if (Camera1Utils.isLanscale(orientation)) {
            previeWidth = height;
            previewHeight = width;
        }

        float offsert = previewHeight * 1.0f / previeWidth;
        Log.d(TAG, "zsr getPreviewSize: " + width + " " + height);
        List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
        for (Camera.Size size : sizes) {
            float asradio = size.width * 1.0f / size.height;
            if (offsert == asradio && size.height <= previeWidth && size.width <= previewHeight) {
                //设置这个要根据横竖屏来，且需要预览开启之前，这样拍出来的图片比较清晰。demo用竖屏来了
                Log.d(TAG, "zsr 最终: " + size.width + " " + size.height + " " + mCameraInfo.orientation);
                return size;
            }
        }

        return sizes.get(0);

    }

    public interface PreviewListener {
        void onPreviewFrame(byte[] datas);
    }

    public Camera.Parameters getParameters() {
        return mParameters;
    }


}
