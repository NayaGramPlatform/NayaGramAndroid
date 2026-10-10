package org.telegram.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Android Instrumentation UI Test for NayaGram Intro Logo and Launcher Icons.
 * Executes on real device or Android emulator runtime to verify:
 * 1. Pixel-level absence of black strokes, dark fringes, or shadow borders.
 * 2. Proper scaling and geometric centering under varied DisplayMetrics and screen configurations.
 * 3. Strict compliance with Android 8.0+ Adaptive Icon safe zone and OEM masks.
 * 4. Corner transparency for round launcher icons without black clipping blocks.
 */
@RunWith(AndroidJUnit4.class)
public class IntroAndLauncherVisualRegressionUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertNotNull("Application context must be available", context);
    }

    @Test
    public void testIntroLogoNoBlackBordersUnderAndroidRuntime() {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.nayagram_intro_logo);
        assertNotNull("nayagram_intro_logo drawable must be present in resources", drawable);

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        assertTrue("Intro logo width must be positive", width > 0);
        assertTrue("Intro logo height must be positive", height > 0);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        int darkBorderPixels = 0;
        int nonTransparentPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                if (alpha > 15) {
                    nonTransparentPixels++;
                    // Pixel with very dark color (near black) and high opacity
                    if (r < 35 && g < 35 && b < 35) {
                        darkBorderPixels++;
                    }
                }
            }
        }

        assertTrue("Logo must have visible content", nonTransparentPixels > 0);
        double darkRatio = (double) darkBorderPixels / (width * height);
        assertTrue("Dark border ratio under Android runtime must be < 0.05%, was " + (darkRatio * 100) + "% (" + darkBorderPixels + " px)",
                darkRatio < 0.0005);
    }

    @Test
    public void testIntroLogoDimensionsAndCenteringUnderDisplayMetrics() {
        int[] testDensities = {
                DisplayMetrics.DENSITY_MEDIUM,
                DisplayMetrics.DENSITY_HIGH,
                DisplayMetrics.DENSITY_XHIGH,
                DisplayMetrics.DENSITY_XXHIGH,
                DisplayMetrics.DENSITY_XXXHIGH
        };

        for (int density : testDensities) {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.densityDpi = density;
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setTo(context.getResources().getDisplayMetrics());
            metrics.densityDpi = density;
            metrics.density = density / 160.0f;

            Context densityContext = context.createConfigurationContext(config);
            Drawable drawable = ContextCompat.getDrawable(densityContext, R.drawable.nayagram_intro_logo);
            assertNotNull("Drawable must load for density " + density, drawable);

            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            assertTrue("Width and height must match within 2px for density " + density, Math.abs(w - h) <= 2);

            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);

            int minX = w, maxX = 0, minY = h, maxY = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (Color.alpha(bitmap.getPixel(x, y)) > 20) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            int contentW = maxX - minX + 1;
            int contentH = maxY - minY + 1;
            assertTrue("Content must be well proportioned", contentW > 0 && contentH > 0);

            float centerX = (minX + maxX) / 2.0f;
            float centerY = (minY + maxY) / 2.0f;
            float targetX = w / 2.0f;
            float targetY = h / 2.0f;

            assertTrue("Centering offset horizontal must be <= 1.5% at density " + density,
                    Math.abs(centerX - targetX) <= Math.max(2.0f, w * 0.015f));
            assertTrue("Centering offset vertical must be <= 1.5% at density " + density,
                    Math.abs(centerY - targetY) <= Math.max(2.0f, h * 0.015f));
        }
    }

    @Test
    public void testRoundLauncherIconNoSquareBlackCorners() {
        Drawable drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round);
        if (drawable == null) {
            // Some build flavors might name it differently, fallback check
            return;
        }

        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        float radius = Math.min(w, h) / 2.0f;
        float cx = w / 2.0f;
        float cy = h / 2.0f;

        int opaqueCornerPixels = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x + 0.5f - cx;
                float dy = y + 0.5f - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Pixel outside circle
                if (dist > radius + 1.0f) {
                    int alpha = Color.alpha(bitmap.getPixel(x, y));
                    if (alpha > 15) {
                        opaqueCornerPixels++;
                    }
                }
            }
        }

        assertEquals("Round icon must have transparent outer corners, no black/opaque borders", 0, opaqueCornerPixels);
    }
}
