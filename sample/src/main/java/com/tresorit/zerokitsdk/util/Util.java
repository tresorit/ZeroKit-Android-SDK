package com.tresorit.zerokitsdk.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Util {
    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }
}
