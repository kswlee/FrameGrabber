package com.tam.media;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

public class VideoDecoder {
	final static String TAG = "VideoDecoder";
	final static String VIDEO_MIME_PREFIX = "video/"; 
	
	private MediaExtractor mMediaExtractor = null;
	private MediaCodec mMediaCodec = null;
	
	private Surface mSurface = null;
	private String mPath = null;	
	private int mVideoTrackIndex = -1;
	
	public VideoDecoder(String path, Surface surface) {
		mPath = path;
		mSurface = surface;
		
		initCodec();
	}
	
	public boolean prepare(long time) {
		return decodeFrameAt(time);
	}
	
	public void startDecode() {		
	}
	
	public void release() {
		if (null != mMediaCodec) {
			mMediaCodec.stop();
			mMediaCodec.release();
		}
		
		if (null != mMediaExtractor) {
			mMediaExtractor.release();
		}
	}
	
	private boolean initCodec() {
		Log.i(TAG, "initCodec");
		mMediaExtractor = new MediaExtractor();
		try {
			mMediaExtractor.setDataSource(mPath);
		} catch (IOException e) {			
			e.printStackTrace();
			return false;
		}
		
		int trackCount = mMediaExtractor.getTrackCount();
		for (int i = 0; i < trackCount; ++i) {
			MediaFormat mf = mMediaExtractor.getTrackFormat(i);
			String mime = mf.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith(VIDEO_MIME_PREFIX)) {
				mVideoTrackIndex = i;
				break;
			}
		}
		if (mVideoTrackIndex < 0) 
			return false;
		
		mMediaExtractor.selectTrack(mVideoTrackIndex);
		MediaFormat mf = mMediaExtractor.getTrackFormat(mVideoTrackIndex);
		String mime = mf.getString(MediaFormat.KEY_MIME);
		mMediaCodec = MediaCodec.createDecoderByType(mime);
		
		mMediaCodec.configure(mf, mSurface, null, 0);
		mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
		mMediaCodec.start();
		Log.i(TAG, "initCodec end");
		
		return true;
	}
	
	private boolean mIsInputEOS = false;
	private boolean decodeFrameAt(long timeUs) {
		Log.i(TAG, "decodeFrameAt " + timeUs);
		mMediaExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
				
		mIsInputEOS = false;
		CodecState inputState = new CodecState();
		CodecState outState = new CodecState();
		boolean reachTarget = false;
		for (;;) {			
			if (!inputState.EOS)
				handleCodecInput(inputState);
						
			if (inputState.outIndex < 0) {
				handleCodecOutput(outState);
				reachTarget = processOutputState(outState, timeUs);
			} else {
				reachTarget = processOutputState(inputState, timeUs);
			}
			
			if (true == reachTarget || outState.EOS) {
				Log.i(TAG, "decodeFrameAt " + timeUs + " reach target or EOS");
				break;
			}
			
			inputState.outIndex = -1;
			outState.outIndex = -1;
		}
		
		return reachTarget;
	}
	
	private boolean processOutputState(CodecState state, long timeUs) {
		if (state.outIndex < 0) 
			return false;
		
		if (state.outIndex >= 0 && state.info.presentationTimeUs < timeUs) {
			Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs);
			mMediaCodec.releaseOutputBuffer(state.outIndex, false);
			return false;
		}
		
		if (state.outIndex >= 0) {
			Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs);
			mMediaCodec.releaseOutputBuffer(state.outIndex, true);
			return true;
		}
		
		return false;
	}
	
	private class CodecState {
		int outIndex = -1;
		BufferInfo info = new BufferInfo();
		boolean EOS = false;
	}
	
	private void handleCodecInput(CodecState state) {		
		ByteBuffer [] inputBuffer = mMediaCodec.getInputBuffers();		
		
		for (;!mIsInputEOS;) {
			int inputBufferIndex = mMediaCodec.dequeueInputBuffer(10000);
			if (inputBufferIndex < 0) 
				continue;
			
			ByteBuffer in = inputBuffer[inputBufferIndex];
			int readSize = mMediaExtractor.readSampleData(in, 0);
			long presentationTimeUs = mMediaExtractor.getSampleTime();
			int flags = mMediaExtractor.getSampleFlags();
													
			boolean EOS = !mMediaExtractor.advance();
			EOS |= (readSize <= 0);
			EOS |= ((flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0);
			
			Log.i(TAG, "input presentationTimeUs " + presentationTimeUs + " isEOS " + EOS);
			
			if (EOS && readSize < 0) 
				readSize = 0;
			
			if (readSize > 0 || EOS) 
				mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, presentationTimeUs, flags | (EOS? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0));
			
			if (EOS) {
				state.EOS = true;
				mIsInputEOS = true;
				break;
			}
						
			state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000);
			if (state.outIndex >= 0) 
				break;
		}
	}
	
	private void handleCodecOutput(CodecState state) {
		state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000);
		if (state.outIndex < 0) {
			return;
		}
		
		if ((state.info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {	
			state.EOS = true;
			Log.i(TAG, "reach output EOS " + state.info.presentationTimeUs);
		}
	}
}
