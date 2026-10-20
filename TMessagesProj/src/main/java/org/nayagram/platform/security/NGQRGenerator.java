package org.nayagram.platform.security;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generates an authentic, scannable NayaGram Authenticator QR Code
 * incorporating official NayaGram brand colors: Blue (#2D7FF9), Cyan (#00D4FF), Purple (#7C3AED), and White (#FFFFFF).
 * Uses high error correction (ErrorCorrectionLevel.H) so that the centered NayaGram Protection shield badge
 * retains 100% scannability for standard authenticator apps (Google Authenticator, Aegis, Bitwarden, etc.).
 */
public final class NGQRGenerator {

    public static final int COLOR_BLUE = 0xFF2D7FF9;   // NayaGram Blue
    public static final int COLOR_CYAN = 0xFF00D4FF;   // Cyan Accent
    public static final int COLOR_PURPLE = 0xFF7C4DFF; // Purple Accent
    public static final int COLOR_WHITE = 0xFFFFFFFF;

    private NGQRGenerator() {}

    public static Bitmap generate(Context context, String otpAuthUri, int targetSize) throws Exception {
        if (targetSize <= 0) targetSize = 720;

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        QRCode code = Encoder.encode(otpAuthUri, ErrorCorrectionLevel.H, hints);
        ByteMatrix input = code.getMatrix();
        if (input == null) throw new IllegalStateException("Could not generate QR matrix");

        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();

        int quietZone = 2; // Quiet zone in modules
        int totalModules = inputWidth + quietZone * 2;
        int multiple = Math.max(1, targetSize / totalModules);
        int finalSize = multiple * totalModules;

        Bitmap bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(COLOR_WHITE);

        Paint modulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint eyeOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint eyeInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint badgeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        badgeBgPaint.setColor(COLOR_WHITE);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        badgeStrokePaint.setColor(COLOR_CYAN);
        badgeStrokePaint.setStyle(Paint.Style.STROKE);
        badgeStrokePaint.setStrokeWidth(AndroidUtilities.dp(2.5f));

        // Center cutout for NayaGram shield badge (approx 20-22% of modules)
        int centerModules = 7;
        if (centerModules % 2 != inputWidth % 2) centerModules++;
        int centerStartX = (inputWidth - centerModules) / 2;
        int centerStartY = (inputHeight - centerModules) / 2;

        RectF moduleRect = new RectF();

        // Draw regular data modules with rounded shapes and subtle color transition
        for (int y = 0; y < inputHeight; y++) {
            for (int x = 0; x < inputWidth; x++) {
                if (input.get(x, y) != 1) continue;

                // Skip modules in the 3 finder pattern areas (7x7)
                if (isFinderArea(x, y, inputWidth, inputHeight)) continue;

                // Skip modules occluded by the center shield badge
                if (x >= centerStartX && x < centerStartX + centerModules &&
                    y >= centerStartY && y < centerStartY + centerModules) {
                    continue;
                }

                float px = (quietZone + x) * multiple;
                float py = (quietZone + y) * multiple;
                moduleRect.set(px, py, px + multiple, py + multiple);

                // Subtle dynamic palette transition: Blue #2D7FF9 -> Cyan #00D4FF
                float diagRatio = (float) (x + y) / (float) (inputWidth + inputHeight);
                int color = blendColors(COLOR_BLUE, COLOR_CYAN, diagRatio * 0.35f);
                modulePaint.setColor(color);

                float radius = multiple * 0.35f;
                canvas.drawRoundRect(moduleRect, radius, radius, modulePaint);
            }
        }

        // Draw the 3 high-contrast finder patterns (top-left, top-right, bottom-left)
        drawFinderPattern(canvas, quietZone, quietZone, multiple, eyeOuterPaint, eyeInnerPaint);
        drawFinderPattern(canvas, quietZone + inputWidth - 7, quietZone, multiple, eyeOuterPaint, eyeInnerPaint);
        drawFinderPattern(canvas, quietZone, quietZone + inputHeight - 7, multiple, eyeOuterPaint, eyeInnerPaint);

        // Draw center NayaGram Protection shield badge
        float centerPixelX = (quietZone + centerStartX) * multiple;
        float centerPixelY = (quietZone + centerStartY) * multiple;
        float centerPixelSize = centerModules * multiple;
        RectF centerRect = new RectF(centerPixelX, centerPixelY, centerPixelX + centerPixelSize, centerPixelY + centerPixelSize);

        // Draw white rounded background with cyan border
        float badgeRadius = centerPixelSize * 0.30f;
        canvas.drawRoundRect(centerRect, badgeRadius, badgeRadius, badgeBgPaint);
        canvas.drawRoundRect(centerRect, badgeRadius, badgeRadius, badgeStrokePaint);

        // Draw the Protection hero logo inside the badge
        Drawable heroDrawable = ContextCompat.getDrawable(context, R.drawable.nayagram_protection_hero);
        if (heroDrawable != null) {
            int padding = (int) (centerPixelSize * 0.12f);
            heroDrawable.setBounds(
                    (int) (centerRect.left + padding),
                    (int) (centerRect.top + padding),
                    (int) (centerRect.right - padding),
                    (int) (centerRect.bottom - padding)
            );
            heroDrawable.draw(canvas);
        }

        return bitmap;
    }

    private static boolean isFinderArea(int x, int y, int width, int height) {
        // Top-left
        if (x < 7 && y < 7) return true;
        // Top-right
        if (x >= width - 7 && y < 7) return true;
        // Bottom-left
        if (x < 7 && y >= height - 7) return true;
        return false;
    }

    private static void drawFinderPattern(Canvas canvas, int modX, int modY, int multiple, Paint outerPaint, Paint innerPaint) {
        float startX = modX * multiple;
        float startY = modY * multiple;
        float totalSize = 7 * multiple;

        RectF outerRect = new RectF(startX, startY, startX + totalSize, startY + totalSize);
        RectF outerHoleRect = new RectF(startX + multiple, startY + multiple, startX + totalSize - multiple, startY + totalSize - multiple);
        RectF innerRect = new RectF(startX + 2 * multiple, startY + 2 * multiple, startX + totalSize - 2 * multiple, startY + totalSize - 2 * multiple);

        // Outer 7x7 frame in NayaGram Blue with Cyan stroke
        outerPaint.setColor(COLOR_BLUE);
        outerPaint.setStyle(Paint.Style.FILL);
        float outerRadius = multiple * 1.5f;
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, outerPaint);

        // Inner 5x5 cutout (White)
        outerPaint.setColor(COLOR_WHITE);
        float holeRadius = multiple * 1.0f;
        canvas.drawRoundRect(outerHoleRect, holeRadius, holeRadius, outerPaint);

        // Center 3x3 solid eye in NayaGram Blue / Cyan / Purple
        innerPaint.setColor(COLOR_BLUE);
        innerPaint.setStyle(Paint.Style.FILL);
        float innerRadius = multiple * 0.75f;
        canvas.drawRoundRect(innerRect, innerRadius, innerRadius, innerPaint);
    }

    private static int blendColors(int c1, int c2, float ratio) {
        float inv = 1f - ratio;
        float r = Color.red(c1) * inv + Color.red(c2) * ratio;
        float g = Color.green(c1) * inv + Color.green(c2) * ratio;
        float b = Color.blue(c1) * inv + Color.blue(c2) * ratio;
        return Color.rgb((int) r, (int) g, (int) b);
    }
}
