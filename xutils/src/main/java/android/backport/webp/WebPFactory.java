package android.backport.webp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Factory to encode and decode WebP images into Android Bitmap
 *
 * @author Alexey Pelykh
 */
@SuppressWarnings("JniMissingFunction")
public final class WebPFactory {
    private WebPFactory() {
    }

    public static boolean available() {
        return false;
    }
    /**
     * Decodes byte array to bitmap
     *
     * @param data    Byte array with WebP bitmap data
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static Bitmap decodeByteArray(byte[] data, BitmapFactory.Options options) {
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Decodes file to bitmap
     *
     * @param path    WebP file path
     * @param options Options to control decoding. Accepts null
     * @return Decoded bitmap
     */
    public static Bitmap decodeFile(String path, BitmapFactory.Options options) {
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * Encodes bitmap into byte array
     *
     * @param bitmap  Bitmap
     * @param quality Quality, should be between 0 and 100
     * @return Encoded byte array
     */
    public static byte[] encodeBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
        return out.toByteArray();
    }
}
