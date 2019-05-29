package hht.com.sharevideo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;

import hht.com.sharevideo.camera.Camera1;
import hht.com.sharevideo.view.AutoFitTextureView;

public class MainActivity extends AppCompatActivity implements Camera1.PreviewListener {
    private static final String TAG = "MainActivity";

    private AutoFitTextureView mTextureView;
    private Camera1 mCamera;
    private ImageView mImageView;
    private NV21ToBitmap mNv21ToBitmap;
    private Camera.Size mSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.textureview);
        mCamera = Camera1.create(this, this);

        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO},
                1);

        mImageView = findViewById(R.id.hehe);
        mNv21ToBitmap = new NV21ToBitmap(MainActivity.this);



    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTextureView.isAvailable()) {
            mCamera.openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
        mCamera.closeCamera();
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            mCamera.openCamera(width, height);
            mCamera.setSurface(mTextureView);
            mCamera.startPreview(surfaceTexture);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            mCamera.stopPreview();
            mCamera.closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    @Override
    public void onPreviewFrame(byte[] datas) {

        new ImageAsync(datas).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private boolean isTack;

    public void takePic(View view) {
        isTack = true;
    }

    class ImageAsync extends AsyncTask<Void, Void, Bitmap> {

        byte[] datas = null;

        public ImageAsync(byte[] datas) {
            this.datas = datas;
        }


        @Override
        protected Bitmap doInBackground(Void... voids) {
            //只支持 ImageFormat.NV21 和 YImageFormat.YUY2
            mSize = mCamera.getParameters().getPreviewSize();
            int width = mSize.width;
            int height = mSize.height;
            if (width != 0 && height != 0) {
              //  datas = mCamera.rotateYUV420Degree90(datas,width,height);
                return mNv21ToBitmap.nv21ToBitmap(datas, width, height);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mImageView.setImageBitmap(bitmap);
        }
    }




}
