package hht.com.sharevideo.tools;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import hht.com.sharevideo.Constans;

/**
 * Created by smile on 2019/5/31.
 */

public class AhardviewDecode {
    //帧率为30fps
    private static final String TAG = "AhardviewDecode";

    private  MediaCodec mDecode;
    private volatile  boolean isRunning = false;
    private PreviewThread mPreviewThread;
    public AhardviewDecode(SurfaceHolder holder,int width,int height){

        try {
            MediaFormat format = MediaFormat.createVideoFormat(Constans.MIME, width, height);
            format.setInteger(MediaFormat.KEY_ROTATION,90);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constans.BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constans.FPS);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constans.KEY_POINT);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);

            // Get an instance of MediaCodec and give it its Mime type
            mDecode = MediaCodec.createDecoderByType(Constans.MIME);
            // Configure the codec
            mDecode.configure(format, holder.getSurface(), null, 0);
            // Start the codec
            mDecode.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
       // mPreviewThread = new PreviewThread();
    }


    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    public void onDecode(final byte[] datas){
        offerDecoder(datas,datas.length);
    }


    public void releae(){
        isRunning = false;
        if (mDecode != null){
            mDecode.stop();
            mDecode.release();
            mDecode = null;
        }
    }

    private  class  PreviewThread extends  Thread {

        byte[] rtpData = null;
        public void decode(byte[] datas){
            Log.d(TAG, "zsr decode: "+datas);
            rtpData = datas;
        }


        @Override
        public void run() {
            isRunning = true;
            while (isRunning){
                Log.d(TAG, "zsr run: ");
                offerDecoder(rtpData,rtpData.length);
            }
        }
    }

    //解码h264数据
    private void offerDecoder(byte[] input, int length) {
        Log.d(TAG, "offerDecoder: ");
        try {
            ByteBuffer[] inputBuffers = mDecode.getInputBuffers();
            int inputBufferIndex = mDecode.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                try{
                    inputBuffer.put(input, 0, length);
                }catch (Exception e){
                    e.printStackTrace();
                }
                mDecode.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            int outputBufferIndex = mDecode.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                //If a valid surface was specified when configuring the codec,
                //passing true renders this output buffer to the surface.
                mDecode.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mDecode.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Throwable t) {
            Log.d(TAG, "zsr offerDecoder: "+t.toString());
            t.printStackTrace();
        }
    }
}

