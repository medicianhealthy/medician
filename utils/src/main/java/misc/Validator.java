package misc;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;

import java.util.List;

public class Validator {

    public static boolean isValidString(final String string){
        return (null != string) && (!string.isEmpty());
    }

    public static boolean isValidList(final List list){
        return (null != list) && (!list.isEmpty());
    }

    public static boolean isValidAndroidResourceId(final Integer resourceId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Resources.ID_NULL != resourceId;
        }
        return (0 != resourceId);
    }

    public static boolean isValidBitMap(final Bitmap bitmap){
        return ((null != bitmap) && (bitmap.getWidth() > 0) && (bitmap.getHeight() > 0) && !bitmap.isRecycled());
    }

    public static boolean isValidObject(Object object) {
        return null != object;
    }
}
