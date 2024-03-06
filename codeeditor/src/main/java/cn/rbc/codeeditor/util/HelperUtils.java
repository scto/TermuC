package cn.rbc.codeeditor.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import cn.rbc.termuc.R;
import android.widget.*;
import android.annotation.*;

public class HelperUtils {

	private static Toast _t;

    public static float getDpi(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    // create bitmap from vector drawable
    public static Bitmap getBitmap(Context context, int res) {
        Bitmap bitmap = null;
		/* ContextCompat.getDrawable */
        Drawable vectorDrawable = context.getDrawable(res);
        if (vectorDrawable != null) {
            vectorDrawable.setAlpha(210);
            //vectorDrawable.setTint(fetchAccentColor(context));
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
            return bitmap;
        }
        return bitmap;
    }

	public static void show(@NonNull Toast t) {
		if (_t != null)
			_t.cancel();
		(_t = t).show();
	}
}
