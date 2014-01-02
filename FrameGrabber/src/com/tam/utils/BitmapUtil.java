package com.tam.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;

public class BitmapUtil {
	public static void saveBitmap(Bitmap bmp, String path) {
		try {
			FileOutputStream fos = new FileOutputStream(path);
			bmp.compress(CompressFormat.JPEG, 100, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public static Bitmap flip(Bitmap src) {
		Matrix matrix = new Matrix();	    
	    matrix.preScale(1.0f, -1.0f);
	    return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
	}
}
