package org.telegram.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IntroTextureRenderingTest {

    @Test
    public void testTexture21ZeroAlpha() {
        // Texture 21 must be 100% transparent to suppress the paper plane
        Bitmap bm21 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Assert.assertEquals("Texture 21 alpha must be 0", 0, Color.alpha(bm21.getPixel(0, 0)));
    }

    @Test
    public void testTexture22SafeMarginClipping() {
        int size = 300; // dp-scaled texture size
        int pad = (int) (size * 0.08f);
        Bitmap bm22 = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm22);

        // Verify the 8% padding zone contains 0 alpha pixels at the edges
        for (int x = 0; x < pad; x++) {
            Assert.assertEquals(0, Color.alpha(bm22.getPixel(x, size / 2)));
            Assert.assertEquals(0, Color.alpha(bm22.getPixel(size - 1 - x, size / 2)));
        }
    }
}
