package hht.com.sharevideo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import hht.com.sharevideo.camera.Camera1;
import hht.com.sharevideo.tools.AhardviewDecode;
import hht.com.sharevideo.tools.AhardwareEncode;
import hht.com.sharevideo.view.AutoFitTextureView;

public class MainActivity extends AppCompatActivity implements Camera1.PreviewListener, AhardwareEncode.EncodeListener {
    private static final String TAG = "MainActivity";

    private AutoFitTextureView mTextureView;
    private Camera1 mCamera;
    private NV21ToBitmap mNv21ToBitmap;
    private Camera.Size mSize;
    private AhardwareEncode mEncode;
    private SurfaceView mSurfaceview;
    private AhardviewDecode mDecode;

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

        mSurfaceview = findViewById(R.id.surface);
        mNv21ToBitmap = new NV21ToBitmap(MainActivity.this);

        mSurfaceview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "zsr surfaceChanged: ");

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        /*if (mTextureView.isAvailable()) {
            mCamera.openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            mCamera.setSurface(mTextureView);
            mCamera.startPreview(mTextureView.getSurfaceTexture());
            mEncode = new AhardwareEncode(mCamera.getPreviewSize().width,
                   mCamera.getPreviewSize().height,MainActivity.this);
            mEncode.startEncode();
        } else {
        }*/
            mTextureView.setSurfaceTextureListener(mTextureListener);


    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
        mCamera.closeCamera();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mDecode.releae();
        mEncode.releae();
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {



        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            mCamera.openCamera(width, height);
            mCamera.setSurface(mTextureView);
            mCamera.startPreview(surfaceTexture);
            mEncode = new AhardwareEncode(mCamera.getPreviewSize().width,
                    mCamera.getPreviewSize().height,MainActivity.this);

            mDecode = new AhardviewDecode(mSurfaceview.getHolder(),mCamera.getPreviewSize().width,mCamera.getPreviewSize().height);

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
        if (mEncode != null){
            mEncode.onFrame(datas);
        }
       // new ImageAsync(datas).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private boolean isTack;

    public void takePic(View view) {
        isTack = true;
    }

    @Override
    public void EncodeData(byte[] datas) {
       // Log.d(TAG, "zsr EncodeData: "+mEncode);

        if (mDecode != null) {
            mDecode.onDecode(datas);
        }
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
           // mImageView.setImageBitmap(bitmap);
        }
    }




}
