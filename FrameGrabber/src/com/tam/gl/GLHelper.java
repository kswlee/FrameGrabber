package com.tam.gl;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.view.Surface;

public class GLHelper {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    
	private SurfaceTexture mSurfaceTexture;
	private TextureRender mTextureRender;
		
	private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
	private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
	private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;		
	
	public void init(SurfaceTexture st) {
		mSurfaceTexture = st;
		initGL();
		
		makeCurrent();
		mTextureRender = new TextureRender();
	}
	
	private void initGL() {
		mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
		if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
			throw new RuntimeException("eglGetdisplay failed : " + 
					GLUtils.getEGLErrorString(EGL14.eglGetError()));
		}
		
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
        	mEglDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }		

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
		
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        GLUtil.checkEglError("eglCreateContext");
        if (mEglContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, configs[0], new Surface(mSurfaceTexture),
                surfaceAttribs, 0);
        GLUtil.checkEglError("eglCreateWindowSurface");
        if (mEglSurface == null) {
            throw new RuntimeException("surface was null");
        }
	}
	
	public void release() {
		if (null != mSurfaceTexture)
			mSurfaceTexture.release();
	}
	
    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }	
	
	public int createOESTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        GLUtil.checkEglError("glBindTexture textureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLUtil.checkEglError("glTexParameter");
        
        return textureID;
	}
	
	public void drawFrame(SurfaceTexture st, int textureID) {
		st.updateTexImage();
		if (null != mTextureRender)
			mTextureRender.drawFrame(st, textureID);
	}
	
	public Bitmap readPixels(int width, int height) {
		ByteBuffer PixelBuffer = ByteBuffer.allocateDirect(4 * width * height);	    
	    PixelBuffer.position(0);
		GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, PixelBuffer);
		
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		PixelBuffer.position(0);		
		bmp.copyPixelsFromBuffer(PixelBuffer);		

		return bmp;
	}	
}
