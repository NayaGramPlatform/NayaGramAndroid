package org.nayagram.platform.security;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * Renders a crystal ice block shield for blocked APK/ZIP message bubbles.
 * Features:
 * - Translucent glacial ice gradient body
 * - Crystalline ice border & corner facet accents
 * - Soft top glacial reflection highlight
 */
public final class IceShieldRenderer {

    private static final Paint icePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    static {
        strokePaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStyle(Paint.Style.FILL);
    }

    public static void drawIceBlock(Canvas canvas, int left, int top, int right, int bottom, float alpha) {
        if (canvas == null || left >= right || top >= bottom) return;

        boolean isDark = Theme.isCurrentThemeDark();
        float radius = AndroidUtilities.dp(16);
        rectF.set(left, top, right, bottom);

        // 1. Glacial translucent crystal body
        int topColor = isDark ? 0x880284C7 : 0x77BAE6FD;
        int bottomColor = isDark ? 0x990369A1 : 0x887DD3FC;
        icePaint.setShader(new LinearGradient(left, top, left, bottom, topColor, bottomColor, Shader.TileMode.CLAMP));
        icePaint.setAlpha((int) (alpha * 240));
        canvas.drawRoundRect(rectF, radius, radius, icePaint);

        // 2. Crystal edge stroke (vivid frost cyan border)
        strokePaint.setStrokeWidth(AndroidUtilities.dp(1.8f));
        strokePaint.setColor(isDark ? 0xDD38BDF8 : 0xEE0284C7);
        strokePaint.setAlpha((int) (alpha * 255));
        canvas.drawRoundRect(rectF, radius, radius, strokePaint);

        // 3. Top glacial reflection line
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setAlpha((int) (alpha * (isDark ? 90 : 160)));
        float inset = AndroidUtilities.dp(8);
        RectF highlightRect = new RectF(left + inset, top + AndroidUtilities.dp(2), right - inset, top + AndroidUtilities.dp(4.5f));
        canvas.drawRoundRect(highlightRect, AndroidUtilities.dp(2), AndroidUtilities.dp(2), highlightPaint);
    }
}
