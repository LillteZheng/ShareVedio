package hht.com.sharevideo.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;

import hht.com.sharevideo.Constans;

public class Camera1Utils {
    public static int getCameraDisplayOrientation(Camera.CameraInfo info, Activity activity){
        int orientation = activity.getWindow().getWindowManager().getDefaultDisplay().getOrientation();
        int degress = 0;
        switch (orientation){
            case Surface.ROTATION_0:
                degress = 0;
                break;
            case Surface.ROTATION_90:
                degress = 90;
                break;
            case Surface.ROTATION_180:
                degress = 180;
                break;
            case Surface.ROTATION_270:
                degress = 270;
                break;
        }
        int result;
        //前置
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degress) % 360;
            result = (360 - result) % 360;  // 镜像
        } else {  // 后置
            result = (info.orientation - degress + 360) % 360;
        }
        //camera.setDisplayOrientation(result);
        return result;

    }

    public static boolean  isLanscale(int oritation){
        return oritation == Constans.LANDSCAPE_90 ||
                oritation == Constans.LANDSCAPE_270;
    }

}
