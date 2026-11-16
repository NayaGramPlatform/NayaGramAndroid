package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

public class AiButtonDrawable extends Drawable {

    private final Drawable base;
    private final Drawable star;

    private final AnimatedFloat animation = new AnimatedFloat(this::invalidateSelf, 0, 1200L, CubicBezierInterpolator.EASE_OUT_QUINT);

    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix gradientMatrix = new Matrix();

    private LinearGradient shimmerShader;
    private RadialGradient haloShader;
    private int cachedW = -1;
    private int cachedH = -1;
    private int alpha = 255;
    private boolean vibrant = true;
    private ColorFilter fallbackFilter;

    private static final int[] AI_COLORS = new int[]{
        0xFF00E5FF, // bright cyan
        0xFF3B82F6, // electric blue
        0xFF8B5CF6, // vivid violet
        0xFFEC4899, // radiant magenta
        0xFF00E5FF  // wrap smoothly
    };

    public AiButtonDrawable(Context context) {
        base = context.getResources().getDrawable(R.drawable.input_ai).mutate();
        star = context.getResources().getDrawable(R.drawable.input_ai_star).mutate();
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dpf2(1.25f));
        gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    public void setVibrant(boolean vibrant) {
        this.vibrant = vibrant;
        invalidateSelf();
    }

    private void ensureShaders(Rect bounds) {
        int w = Math.max(1, bounds.width());
        int h = Math.max(1, bounds.height());
        if (w == cachedW && h == cachedH && shimmerShader != null && haloShader != null) {
            return;
        }
        cachedW = w;
        cachedH = h;
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        float radius = Math.min(w, h) * 0.5f;
        haloShader = new RadialGradient(
            cx, cy, Math.max(1f, radius),
            new int[]{0x4238BDF8, 0x268B5CF6, 0x00EC4899},
            new float[]{0f, 0.58f, 1f},
            Shader.TileMode.CLAMP
        );
        shimmerShader = new LinearGradient(
            bounds.left, bounds.top, bounds.left + w * 1.35f, bounds.top + h * 1.35f,
            AI_COLORS,
            new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
            Shader.TileMode.REPEAT
        );
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty() || alpha <= 0) {
            return;
        }

        final float t = animation.set(1.0f);
        final long now = SystemClock.elapsedRealtime();
        final float cycle = (now % 2800L) / 2800f;
        final float pulse = 0.5f + 0.5f * (float) Math.sin(cycle * Math.PI * 2.0);

        final int pad = Math.min(dp(3), Math.min(bounds.width(), bounds.height()) / 6);
        final int iconLeft = bounds.left + pad;
        final int iconTop = bounds.top + pad;
        final int iconW = Math.max(1, bounds.width() - pad * 2);
        final int iconH = Math.max(1, bounds.height() - pad * 2);

        if (vibrant) {
            ensureShaders(bounds);

            // Soft glowing aurora backdrop + crisp luminous ring
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float glowRadius = Math.min(bounds.width(), bounds.height()) * (0.46f + 0.04f * pulse);
            haloPaint.setShader(haloShader);
            haloPaint.setAlpha((int) (alpha * (0.78f + 0.22f * pulse)));
            canvas.drawCircle(cx, cy, glowRadius, haloPaint);

            gradientMatrix.setTranslate(cycle * bounds.width() * 1.35f, cycle * bounds.height() * 0.65f);
            shimmerShader.setLocalMatrix(gradientMatrix);

            ringPaint.setShader(shimmerShader);
            ringPaint.setAlpha((int) (alpha * (0.34f + 0.24f * pulse)));
            canvas.drawCircle(cx, cy, glowRadius - dp(1), ringPaint);

            layerPaint.setAlpha(alpha);
            int saveCount = canvas.saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, layerPaint);

            base.setColorFilter(null);
            base.setAlpha(255);
            base.setBounds(iconLeft, iconTop, iconLeft + iconW, iconTop + iconH);
            base.draw(canvas);

            drawTwinklingStars(canvas, iconLeft, iconTop, iconW, iconH, t, cycle);

            gradientPaint.setShader(shimmerShader);
            gradientPaint.setAlpha(255);
            canvas.drawRect(bounds, gradientPaint);

            canvas.restoreToCount(saveCount);

            // Bright white specular core inside main sparkle for extra crisp pop
            star.setColorFilter(null);
            star.setAlpha((int) (alpha * (0.55f + 0.45f * pulse)));
            float coreCx = iconLeft + iconW * 0.352f;
            float coreCy = iconTop + iconH * 0.248f;
            float coreR = iconW * (0.055f + 0.025f * pulse);
            star.setBounds((int) (coreCx - coreR), (int) (coreCy - coreR), (int) (coreCx + coreR), (int) (coreCy + coreR));
            star.draw(canvas);
        } else {
            base.setColorFilter(fallbackFilter);
            star.setColorFilter(fallbackFilter);
            base.setAlpha(alpha);
            star.setAlpha(alpha);
            base.setBounds(iconLeft, iconTop, iconLeft + iconW, iconTop + iconH);
            base.draw(canvas);
            drawTwinklingStars(canvas, iconLeft, iconTop, iconW, iconH, t, cycle);
        }

        if (isVisible() && alpha > 0) {
            invalidateSelf();
        }
    }

    private void drawTwinklingStars(Canvas canvas, int left, int top, int w, int h, float t, float cycle) {
        final float shimmer1 = 0.85f + 0.22f * (float) Math.sin(cycle * Math.PI * 2.0);
        final float shimmer2 = 0.85f + 0.22f * (float) Math.sin((cycle + 0.38f) * Math.PI * 2.0);

        final float cx1 = left + w * 0.352f;
        final float cy1 = top + h * 0.248f;
        final float r1 = w * 0.112f * shimmer1 * (float) (1.0f - 0.35f * Math.sin(Math.PI * AndroidUtilities.cascade(t, 0, 2, 1.5f)));

        final float cx2 = left + w * 0.215f;
        final float cy2 = top + h * 0.43f;
        final float r2 = w * 0.095f * shimmer2 * (float) (1.0f - 0.35f * Math.sin(Math.PI * AndroidUtilities.cascade(t, 1, 2, 1.5f)));

        star.setAlpha(255);
        star.setBounds((int) (cx1 - r1), (int) (cy1 - r1), (int) (cx1 + r1), (int) (cy1 + r1));
        star.draw(canvas);

        star.setBounds((int) (cx2 - r2), (int) (cy2 - r2), (int) (cx2 + r2), (int) (cy2 + r2));
        star.draw(canvas);
    }

    public void animate() {
        animation.force(0.0f);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        this.fallbackFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.max(base.getIntrinsicWidth(), dp(32));
    }

    @Override
    public int getIntrinsicHeight() {
        return Math.max(base.getIntrinsicHeight(), dp(32));
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
