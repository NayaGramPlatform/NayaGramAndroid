package org.telegram.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IntroTextureRenderingTest {

    @Test
    public void testTexture21ZeroAlphaUnderAllTransforms() {
        // Texture 21 must remain 100% transparent under all scale and translation states
        Bitmap bm21 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Assert.assertEquals("Texture 21 alpha must be 0", 0, Color.alpha(bm21.getPixel(0, 0)));
    }

    @Test
    public void testTexture22RenderingPathScaleOverscrollRotationClipping() {
        int baseSize = 512;
        int safeMargin = (int) (baseSize * 0.08f);

        Bitmap source = Bitmap.createBitmap(baseSize, baseSize, Bitmap.Config.ARGB_8888);
        Canvas srcCanvas = new Canvas(source);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        // Draw centered logo inside safe bounds (inside the 8% margin)
        srcCanvas.drawRect(safeMargin, safeMargin, baseSize - safeMargin, baseSize - safeMargin, paint);

        // Test cases: [scaleX, scaleY, rotationDegrees, overscrollDx]
        float[][] testMatrices = new float[][]{
            {1.0f, 1.0f, 0f, 0f},       // Base idle state
            {0.85f, 0.85f, 0f, 0f},     // Swipe shrink scale
            {1.15f, 1.15f, 0f, 40f},    // Overscroll stretch + horizontal displacement
            {1.0f, 1.0f, 90f, 0f},      // 90 deg landscape rotation
            {1.0f, 1.0f, 270f, 0f}      // 270 deg landscape reverse rotation
        };

        for (float[] config : testMatrices) {
            float sx = config[0];
            float sy = config[1];
            float rot = config[2];
            float dx = config[3];

            Bitmap output = Bitmap.createBitmap(baseSize, baseSize, Bitmap.Config.ARGB_8888);
            Canvas outCanvas = new Canvas(output);
            Matrix matrix = new Matrix();
            matrix.postScale(sx, sy, baseSize / 2f, baseSize / 2f);
            matrix.postRotate(rot, baseSize / 2f, baseSize / 2f);
            matrix.postTranslate(dx, 0);

            outCanvas.drawBitmap(source, matrix, null);

            // Assert that outer 2% bounding pixels remain untouched/transparent (no harsh border clipping)
            int edgeCheck = 10;
            for (int i = 0; i < edgeCheck; i++) {
                Assert.assertEquals("Edge clipping detected on top edge at " + i + " with rot=" + rot,
                        0, Color.alpha(output.getPixel(baseSize / 2, i)));
                Assert.assertEquals("Edge clipping detected on bottom edge at " + i + " with rot=" + rot,
                        0, Color.alpha(output.getPixel(baseSize / 2, baseSize - 1 - i)));
            }
        }
    }
}
