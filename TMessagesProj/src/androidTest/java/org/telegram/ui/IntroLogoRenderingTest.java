package org.telegram.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.telegram.messenger.R;

@RunWith(AndroidJUnit4.class)
public class IntroLogoRenderingTest {
    @Test
    public void introLogoRendersInsideBoundsAcrossDensities() {
        Context context = ApplicationProvider.getApplicationContext();
        int[] densities = {DisplayMetrics.DENSITY_LOW, DisplayMetrics.DENSITY_MEDIUM,
                DisplayMetrics.DENSITY_HIGH, DisplayMetrics.DENSITY_XHIGH,
                DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_XXXHIGH};
        for (int density : densities) {
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.densityDpi = density;
            Context densityContext = context.createConfigurationContext(context.getResources()
                    .getConfiguration());
            Drawable logo = densityContext.getDrawable(R.drawable.nayagram_intro_logo);
            assertNotNull("logo drawable at density " + density, logo);
            int target = Math.round(192f * density / DisplayMetrics.DENSITY_DEFAULT);
            Bitmap bitmap = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            int pad = Math.round(target * 0.08f);
            logo.setBounds(pad, pad, target - pad, target - pad);
            logo.draw(canvas);
            int minX = target, minY = target, maxX = -1, maxY = -1;
            for (int y = 0; y < target; y++) {
                for (int x = 0; x < target; x++) {
                    if ((bitmap.getPixel(x, y) >>> 24) != 0) {
                        minX = Math.min(minX, x); minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                    }
                }
            }
            assertTrue("logo should paint pixels at density " + density, maxX >= minX && maxY >= minY);
            assertTrue("logo left margin at density " + density, minX >= pad);
            assertTrue("logo top margin at density " + density, minY >= pad);
            assertTrue("logo right margin at density " + density, maxX < target - pad);
            assertTrue("logo bottom margin at density " + density, maxY < target - pad);
            bitmap.recycle();
        }
    }
}
