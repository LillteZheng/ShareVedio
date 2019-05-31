package hht.com.sharevideo.view;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import hht.com.sharevideo.DataInfo;

/**
 * @author zed
 * @date 2017/11/22 上午10:38
 * @desc
 */

public class MediaCodecSurfaceView extends SurfaceView {

	private final String TAG = MediaCodecSurfaceView.class.getSimpleName();

	//设置解码分辨率
	private final int VIDEO_WIDTH  = 1080;//2592
	private final int VIDEO_HEIGHT = 720;//1520

	//解码帧率 1s解码30帧
	private final int FRAME_RATE = 30;

	//支持格式
	private final String VIDEOFORMAT_H264  = "video/avc";
	private final String VIDEOFORMAT_MPEG4 = "video/mp4v-es";
	private final String VIDEOFORMAT_HEVC  = "video/hevc";

	//默认格式
	private String mMimeType = VIDEOFORMAT_H264;

	//接收的视频帧队列
	private volatile ArrayList<DataInfo> mFrmList = new ArrayList<>();


	private MediaCodec   mMediaCodec;
	private DecodeThread mDecodeThread;
	private Surface      mSurface;


	private int mVideoWidth;
	private int mVideoHeight;


	public MediaCodecSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(mCallback);
	}

	private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {

			Log.i(TAG, "surfaceCreated");
			mSurface = holder.getSurface();
			init();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.i(TAG, "surfaceChanged width = " + width + " height = " + height);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "surfaceDestroyed");
			unInit();
		}
	};


	public void init() {

		Log.i(TAG, "init");

		if (mDecodeThread != null) {
			mDecodeThread.stopThread();
			try {
				mDecodeThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mDecodeThread = null;
		}

		if (mMediaCodec != null) {
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec = null;
		}

		try {
			//通过多媒体格式名创建一个可用的解码器
			mMediaCodec = MediaCodec.createDecoderByType(mMimeType);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Init Exception " + e.getMessage());
		}
		//初始化解码器格式
		MediaFormat mediaformat = MediaFormat.createVideoFormat(mMimeType, VIDEO_WIDTH, VIDEO_HEIGHT);
		//设置帧率
		mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		//crypto:数据加密 flags:编码器/编码器
		mMediaCodec.configure(mediaformat, mSurface, null, 0);
		mMediaCodec.start();
		mDecodeThread = new DecodeThread();
		mDecodeThread.start();
	}

	public void unInit() {
		if (mDecodeThread != null) {
			mDecodeThread.stopThread();
			try {
				mDecodeThread.join(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mDecodeThread = null;
		}
		try {
			if (mMediaCodec != null) {
				mMediaCodec.stop();
				mMediaCodec.release();
				mMediaCodec = null;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		mFrmList.clear();
	}

	public void bindSurface(){

	}


	public int getVideoWidth() {
		return mVideoWidth;
	}

	public int getVideoHeight() {
		return mVideoHeight;
	}



	/**
	 * @author zed
	 * @description 解码线程
	 * @time 2017/11/22
	 */
	private class DecodeThread extends Thread {

		private boolean isRunning = true;

		public synchronized void stopThread() {
			isRunning = false;
		}

		public boolean isRunning() {
			return isRunning;
		}

		@Override
		public void run() {

			Log.i(TAG, "===start DecodeThread===");

			//存放目标文件的数据
			ByteBuffer byteBuffer = null;
			//解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			long startMs = System.currentTimeMillis();
			DataInfo dataInfo = null;
			while (isRunning) {

				if (mFrmList.isEmpty()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				dataInfo = mFrmList.remove(0);

				long startDecodeTime = System.currentTimeMillis();

				//1 准备填充器
				int inIndex = -1;

				try {
					inIndex = mMediaCodec.dequeueInputBuffer(dataInfo.receivedDataTime);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					Log.e(TAG, "IllegalStateException dequeueInputBuffer ");
				}

				if (inIndex >= 0) {
					//2 准备填充数据
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
						byteBuffer = mMediaCodec.getInputBuffers()[inIndex];
						byteBuffer.clear();
					} else {
						byteBuffer = mMediaCodec.getInputBuffer(inIndex);
					}

					if (byteBuffer == null) {
						continue;
					}

					byteBuffer.put(dataInfo.mDataBytes, 0, dataInfo.mDataBytes.length);
					//3 把数据传给解码器
					mMediaCodec.queueInputBuffer(inIndex, 0, dataInfo.mDataBytes.length, 0, 0);

				} else {
					SystemClock.sleep(50);
					continue;
				}

				//这里可以根据实际情况调整解码速度
				long sleep = 50;

				if (mFrmList.size() > 20) {
					sleep = 0;
				}

				SystemClock.sleep(sleep);


				int outIndex = MediaCodec.INFO_TRY_AGAIN_LATER;

				//4 开始解码
				try {
					outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					Log.e(TAG, "IllegalStateException dequeueOutputBuffer " + e.getMessage());
				}

				if (outIndex >= 0) {

					//帧控制
					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					boolean doRender = (info.size != 0);

					//对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
					//调用这个api之后，SurfaceView才有图像
					mMediaCodec.releaseOutputBuffer(outIndex, doRender);


					Log.i(TAG, "DecodeThread delay = " + (System.currentTimeMillis() - dataInfo.receivedDataTime) + " spent = " + (System.currentTimeMillis() - startDecodeTime) + " size = " + mFrmList.size());
					System.gc();

				} else {
					switch (outIndex) {
						case MediaCodec.INFO_TRY_AGAIN_LATER: {

						}
						break;
						case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
							MediaFormat newFormat = mMediaCodec.getOutputFormat();
							mVideoWidth = newFormat.getInteger("width");
							mVideoHeight = newFormat.getInteger("height");

							//是否支持当前分辨率
							/*String support = MediaCodecUtils.getSupportMax(mMimeType);
							if (support != null) {
								String width = support.substring(0, support.indexOf("x"));
								String height = support.substring(support.indexOf("x") + 1, support.length());
								Log.i(TAG, " current " + mVideoWidth + "x" + mVideoHeight + " mMimeType " + mMimeType);
								Log.i(TAG, " Max " + width + "x" + height + " mMimeType " + mMimeType);
								if (Integer.parseInt(width) < mVideoWidth || Integer.parseInt(height) < mVideoHeight) {
									if (mSupportListener != null) {
										mSupportListener.UnSupport();
									}
								}
							}*/
						}
						break;
						case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: {

						}
						break;
						default: {

						}
					}
				}
			}

			Log.i(TAG, "===stop DecodeThread===");
		}

	}

	public void onReceived(byte[] data) {

		DataInfo dataInfo = new DataInfo();
		dataInfo.mDataBytes = data;
		dataInfo.receivedDataTime = System.currentTimeMillis();
		mFrmList.add(dataInfo);
	}

}
