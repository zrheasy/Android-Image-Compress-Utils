package com.zrh.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author zrh
 * @date 2023/7/3
 * <p>
 * 决定图片文件大小的几个因素：
 * 1.像素大小（每个像素占用的大小）
 * 2.分辨率（图片的宽高）
 * 3.编码格式（PNG、JPEG、WEBP）
 * 4.压缩质量
 */
public class ImageCompressUtils {

    /**
     * 获取图片旋转到正确方向的角度
     *
     * @param imageFile 图片文件
     * @return 图片旋转角度
     */
    public static int getRotateDegree(File imageFile) {
        try {
            return getRotateDegree(new ExifInterface(imageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int getRotateDegree(ExifInterface exifInterface) {
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                                        ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
        }
        return 0;
    }

    /**
     * 将图片文件加载到Bitmap并返回Bitmap，加载时限制宽高防止内存溢出
     *
     * @param imageFile 图片文件
     * @param maxWidth  限制最大宽度，0则加载原图宽度
     * @param maxHeight 限制最大高度，0则加载原图高度
     * @return Bitmap
     */
    public static Bitmap getBitmap(File imageFile, int maxWidth, int maxHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int rotate = getRotateDegree(imageFile);
            int width = options.outWidth;
            int height = options.outHeight;
            if (rotate == 90 || rotate == 270) {
                width = options.outHeight;
                height = options.outWidth;
            }

            float withScale = maxWidth == 0 ? 1f : 1f * maxWidth / width;
            float heightScale = maxHeight == 0 ? 1f : 1f * maxHeight / height;
            float scale = Math.min(withScale, heightScale);
            if (scale < 1) {
                options.inSampleSize = (int) (1 / scale);
            }
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            if (rotate > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate * 1f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 压缩图片文件到指定文件夹
     *
     * @param imageFile 图片文件地址
     * @param outputDir 输出目录
     * @param fileName  输出文件名
     * @param config    压缩配置
     * @return 返回压缩后的文件，压缩失败则返回原文件
     */
    public static File compress(File imageFile, File outputDir, String fileName, Config config) {

        Bitmap bitmap = getBitmap(imageFile, config.maxWidth, config.maxHeight);

        if (bitmap == null) return imageFile;

        if (!outputDir.exists() && !outputDir.mkdirs()) return imageFile;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 100;
            bitmap.compress(config.compressFormat, quality, baos);
            while (quality > config.minQuality && baos.size() > config.maxBytesLength) {
                quality -= 10;
                if (quality < config.minQuality) quality = config.minQuality;
                baos.reset();
                bitmap.compress(config.compressFormat, quality, baos);
            }

            File file = new File(outputDir, fileName);
            FileOutputStream fos = new FileOutputStream(file.getAbsoluteFile());
            baos.writeTo(fos);
            fos.flush();
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageFile;
    }

    public static class Config {
        public int maxWidth;
        public int maxHeight;
        public int minQuality = 100;
        public int maxBytesLength = 5 * 1024 * 1024;
        public Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
    }
}
