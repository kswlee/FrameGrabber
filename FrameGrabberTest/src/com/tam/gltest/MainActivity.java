package com.tam.gltest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.tam.media.FrameGrabber;
import com.tam.utils.BitmapUtil;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private final static String TAG = "MainActivity";
	@SuppressLint("SdCardPath")
	private final static String VIDEO_CONTENT = "/sdcard/frameCount.mp4";
    
	private FrameGrabber mFrameGrabber = null;
	private Button mButton = null;
	private ImageView mImageView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
								
		// UI Setup
		initUIControls();
	}
		
	private void initUIControls() {
		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				testFrameGrabber();								
			}			
		});
		
		mImageView = (ImageView) findViewById(R.id.imageView);		
	}
		
	@SuppressLint("SdCardPath")
	private void testFrameGrabber() {
		File videoFile = new File(VIDEO_CONTENT);
		if (false == videoFile.exists()) {
			copyAssets();
		}		
		
		boolean useMMDR = false;
		
		String video = VIDEO_CONTENT;	
		int captureFrame = 5;
		long captureBegin = System.currentTimeMillis();			
		Bitmap bmp = useMMDR? getFrameAtTimeByMMDR(video, 33333 * captureFrame) : getFrameAtTimeByFrameGrabber(video, 33333 * captureFrame);				
		long captureSpends = System.currentTimeMillis() - captureBegin;
		
		Log.i(TAG, "grab frame spends " + captureSpends + "ms");
			
		if (null != bmp && null != mImageView) {
			mImageView.setImageBitmap(bmp);
		} else if (null != bmp)  
			BitmapUtil.saveBitmap(bmp, String.format("/sdcard/read_%d.jpg", captureFrame));
				
		if (null != mFrameGrabber)
			mFrameGrabber.release();
	}	
	
	private Bitmap getFrameAtTimeByMMDR(String path, long time) {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
	    mmr.setDataSource(path);
	    Bitmap bmp = mmr.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
	    mmr.release();		
	    return bmp;
	}
	
	private Bitmap getFrameAtTimeByFrameGrabber(String path, long time) {
		mFrameGrabber = new FrameGrabber();
		mFrameGrabber.setDataSource(path);
		mFrameGrabber.setTargetSize(1280, 720);
		mFrameGrabber.init();						
		return mFrameGrabber.getFrameAtTime(time);		
	}	
	
	private void copyAssets() {
	    AssetManager assetManager = getAssets();
	    String[] files = null;
	    try {
	        files = assetManager.list("");
	    } catch (IOException e) {
	        Log.e(TAG, "Failed to get asset file list.", e);
	    }
	    for(String filename : files) {
	        InputStream in = null;
	        OutputStream out = null;
	        try {
	          in = assetManager.open(filename);
	          File outFile = new File(VIDEO_CONTENT);
	          out = new FileOutputStream(outFile);
	          copyFile(in, out);
	          in.close();
	          in = null;
	          out.flush();
	          out.close();
	          out = null;
	        } catch(IOException e) {
	            Log.e(TAG, "Failed to copy asset file: " + filename, e);
	        }       
	    }
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}		
}
