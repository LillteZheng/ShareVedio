package hht.com.sharevideo.tools;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import hht.com.sharevideo.Constans;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;

/**
 * Created by smile on 2019/5/29.
 */

public class AhardwareEncode {
    //帧率为30fps
    private static final String TAG = "AhardwareEncode";
    private  MediaCodec mCodec;
    private int mWidth,mHeight;
    private EncodeListener mEncodeListener;
    private volatile boolean isRunning = false;
    private EncodeThread mEncodeThread;
    private byte[] yuv420 = null;
    byte[] m_info = null;
    byte[] h264 = null;
    public AhardwareEncode(int width,int height,EncodeListener listener){
        mEncodeListener = listener;
        mWidth = width;
        mHeight = height;
        try {
            MediaFormat format = MediaFormat.createVideoFormat(Constans.MIME,mWidth,mHeight);
            //设置颜色格式
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo
                    .CodecCapabilities.COLOR_FormatYUV420Flexible);
            //设置视频码率
            format.setInteger(MediaFormat.KEY_BIT_RATE,Constans.BIT_RATE);
            //设置视频 fps
            format.setInteger(MediaFormat.KEY_FRAME_RATE,Constans.FPS);
            //设置关键帧，2秒获取一次
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,Constans.KEY_POINT);

            format.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);

            //创建 mediacode
            mCodec = MediaCodec.createEncoderByType(Constans.MIME);
            //先config
            mCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            //start 进入等待状态
            mCodec.start();

            yuv420 = new byte[width*height*3/2];
            h264 = new byte[width*height*3];

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class EncodeThread extends Thread{
        volatile byte[] datas = null;
        public void onEncode(byte[] datas){
            this.datas = datas;
        }

        @Override
        public void run() {
            super.run();
            if (datas != null) {
                isRunning = true;
                while(isRunning) {
                  /*  int pos = 0;
                    swapYV12toI420(datas, yuv420, mWidth, mHeight);
                    try {
                        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
                        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(datas);
                            mCodec.queueInputBuffer(inputBufferIndex, 0, datas.length, 0, 0);

                        }

                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo,0);

                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            byte[] outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);

                            if(m_info != null){
                                System.arraycopy(outData, 0,  h264, pos, outData.length);
                                pos += outData.length;

                            }else{//保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
                                ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                                Log.v("xmc", "swapYV12toI420:outData:"+outData);
                                Log.v("xmc", "swapYV12toI420:spsPpsBuffer:"+spsPpsBuffer);
//
                                for(int i=0;i<outData.length;i++){
                                    Log.e("xmc333", "run: get data rtpData[i]="+i+":"+outData[i]);//输出SPS和PPS循环
                                }

                                if (spsPpsBuffer.getInt() == 0x00000001) {
                                    m_info = new byte[outData.length];
                                    System.arraycopy(outData, 0, m_info, 0, outData.length);
                                }else {
                                    return ;
                                }
                            }
                            if(h264[4] == 0x65) {//key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上
                                System.arraycopy(m_info, 0,  h264, 0, m_info.length);
                                System.arraycopy(outData, 0,  h264, m_info.length, outData.length);
                            }
                            if (mEncodeListener != null){
                                mEncodeListener.EncodeData(outData);
                            }
                            mCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }*/
                    try {
                        // byte[] nv12 = new byte[mWidth * mHeight * 3 / 2];
                        // NV21ToNV12(datas, nv12, mWidth, mHeight);
                        //拿到输入流队列数组
                        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                        //从输入队列中拿到数据
                        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            //获取需要编码的输入流队列
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(datas);
                            //把数据流入输入流，到native去编码
                            mCodec.queueInputBuffer(inputBufferIndex, 0, datas.length, System.currentTimeMillis(), 0);
                        }

                        //获取编码后的数据
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
                        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            byte[] outData = new byte[outputBuffer.remaining()];
                            //把编码后的数据给 outData
                            outputBuffer.get(outData, 0, outData.length);
                            if (mEncodeListener != null) {
                                mEncodeListener.EncodeData(outData);
                            }
                            mCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }else{
                Log.d(TAG, "zsr run 数据为null");
            }

        }
    }

    public void onFrame( byte[] datas){

       /* if (mYuv420Queue.size() >= 10){
            mYuv420Queue.poll();
        }
        mYuv420Queue.add(datas);*/

        if (mEncodeThread == null){
            mEncodeThread = new EncodeThread();
            mEncodeThread.start();
        }
        mEncodeThread.onEncode(datas);

    }

    private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        Log.v("xmc", "NV21toI420SemiPlanar:::"+width+"+"+height);
        final int iSize = width * height;
        System.arraycopy(nv21bytes, 0, i420bytes, 0, iSize);

        for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
            i420bytes[iSize + iIndex / 2 + iSize / 4] = nv21bytes[iSize + iIndex]; // U
            i420bytes[iSize + iIndex / 2] = nv21bytes[iSize + iIndex + 1]; // V
        }
    }


    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    public interface EncodeListener{
        void EncodeData(byte[] datas);
    }

    public void releae(){
        isRunning = false;
        if (mCodec != null){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
        mEncodeThread = null;
    }
    //yv12 转 yuv420p  yvu -> yuv
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width*height);
        System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);
        System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);
    }
}
