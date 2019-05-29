package hht.com.sharevideo.tools;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by smile on 2019/5/29.
 */

public class AhardwareEncode {
    //使用H.265编码
    private static String MIME = MediaFormat.MIMETYPE_VIDEO_HEVC;
    private final MediaCodec mMediaCodec;

    public AhardwareEncode(int width,int height){
        try {
            mMediaCodec = MediaCodec.createDecoderByType(MIME);
            MediaFormat format = new MediaFormat();
            //视频格式
            format.setString(MediaFormat.KEY_MIME,MIME);
            //视频宽度
            format.setInteger(MediaFormat.KEY_WIDTH,width);
            //视频高度
            format.setInteger(MediaFormat.KEY_HEIGHT,height);
            //设置颜色格式
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
            //设置视频码率
            format.setInteger(MediaFormat.KEY_BIT_RATE,125000);
            //设置视频 fps
            format.setInteger(MediaFormat.KEY_FRAME_RATE,30);
            //设置关键帧，2秒获取一次
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);
            mMediaCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onFrame(byte[] datas){
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
    }
}
