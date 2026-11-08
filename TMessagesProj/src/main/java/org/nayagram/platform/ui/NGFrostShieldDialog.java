package org.nayagram.platform.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.NGSettingsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Winter Frost Security Shield Dialog:
 * Intercepts taps on incoming blocked files (APK, ZIP, executables, scripts).
 * Renders an active real-time snowfall particle animation ("শীতের তুষার ইফেক্ট"),
 * icy cyan glassmorphic container, frozen file badge, and clear security guidance.
 */
public class NGFrostShieldDialog {

    private static class Snowflake {
        float x;
        float y;
        float speed;
        float size;
        float alpha;
        float angle;
        float swaySpeed;

        Snowflake(int width, int height, Random random) {
            reset(width, height, random);
            y = random.nextFloat() * (height > 0 ? height : 400);
        }

        void reset(int width, int height, Random random) {
            x = random.nextFloat() * (width > 0 ? width : 600);
            y = -10;
            speed = 1.2f + random.nextFloat() * 3.2f;
            size = 3.0f + random.nextFloat() * 6.5f;
            alpha = 0.35f + random.nextFloat() * 0.65f;
            angle = random.nextFloat() * (float) Math.PI * 2f;
            swaySpeed = 0.02f + random.nextFloat() * 0.04f;
        }

        void update(int width, int height, Random random) {
            y += speed;
            angle += swaySpeed;
            x += (float) Math.sin(angle) * 1.2f;
            if (y > height + 10) {
                reset(width, height, random);
            }
        }
    }

    private static class SnowfallView extends View {
        private final List<Snowflake> snowflakes = new ArrayList<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint crystalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private boolean initialized = false;

        public SnowfallView(Context context) {
            super(context);
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);

            crystalPaint.setColor(0xFF80D8FF);
            crystalPaint.setTextSize(AndroidUtilities.dp(16));
            crystalPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0 && h > 0 && !initialized) {
                snowflakes.clear();
                for (int i = 0; i < 45; i++) {
                    snowflakes.add(new Snowflake(w, h, random));
                }
                initialized = true;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            for (int i = 0; i < snowflakes.size(); i++) {
                Snowflake flake = snowflakes.get(i);
                flake.update(w, h, random);
                paint.setAlpha((int) (flake.alpha * 255));
                canvas.drawCircle(flake.x, flake.y, flake.size, paint);
                if (i % 6 == 0) {
                    crystalPaint.setAlpha((int) (flake.alpha * 180));
                    canvas.drawText("❄", flake.x, flake.y + flake.size, crystalPaint);
                }
            }

            postInvalidateOnAnimation();
        }
    }

    public static void show(Context context, String fileName) {
        if (context == null) {
            return;
        }
        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            FrameLayout root = new FrameLayout(context);
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Background card with Icy Frost gradient
            GradientDrawable cardBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF0B192C, 0xFF1E3E62, 0xFF003049}
            );
            cardBg.setCornerRadius(AndroidUtilities.dp(24));
            cardBg.setStroke(AndroidUtilities.dp(2), 0xFF00E5FF);
            root.setBackground(cardBg);
            root.setClipToOutline(true);

            // Real-time Snowfall Particle View
            SnowfallView snowfallView = new SnowfallView(context);
            root.addView(snowfallView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // Dialog Content Layout
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = AndroidUtilities.dp(20);
            content.setPadding(pad, pad + AndroidUtilities.dp(4), pad, pad);

            // Shimmering Snowflake Icons
            TextView iconView = new TextView(context);
            iconView.setText("❄️ ❆ ❅");
            iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            iconView.setGravity(Gravity.CENTER);
            content.addView(iconView);

            // Title
            TextView titleView = new TextView(context);
            titleView.setText("NayaGram Frost Shield");
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            titleView.setTextColor(0xFF00E5FF);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(2));
            content.addView(titleView);

            // Subtitle
            TextView subtitleView = new TextView(context);
            subtitleView.setText("File Safely Frozen for Security");
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            subtitleView.setTextColor(0xFFB0BEC5);
            subtitleView.setGravity(Gravity.CENTER);
            content.addView(subtitleView);

            // Blocked File Name Badge
            TextView fileBadge = new TextView(context);
            fileBadge.setText(fileName != null ? "🧊 " + fileName : "🧊 Blocked Archive / Package");
            fileBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            fileBadge.setTextColor(0xFFE0F7FA);
            fileBadge.setGravity(Gravity.CENTER);
            fileBadge.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(0x3300E5FF);
            badgeBg.setCornerRadius(AndroidUtilities.dp(12));
            badgeBg.setStroke(AndroidUtilities.dp(1), 0x6600E5FF);
            fileBadge.setBackground(badgeBg);
            LinearLayout.LayoutParams fbParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            fbParams.topMargin = AndroidUtilities.dp(14);
            fbParams.bottomMargin = AndroidUtilities.dp(14);
            content.addView(fileBadge, fbParams);

            // Explanation Text
            TextView descView = new TextView(context);
            descView.setText("Incoming APK, ZIP, scripts, and executable archives are automatically frozen to shield your phone from malware, background stealth installers, and malicious code payloads.");
            descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            descView.setTextColor(0xFFCFD8DC);
            descView.setGravity(Gravity.CENTER);
            descView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            content.addView(descView);

            // Buttons Container
            LinearLayout buttonsLayout = new LinearLayout(context);
            buttonsLayout.setOrientation(LinearLayout.VERTICAL);
            buttonsLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams blParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            blParams.topMargin = AndroidUtilities.dp(18);
            content.addView(buttonsLayout, blParams);

            // Primary Dismiss Button
            TextView btnDismiss = new TextView(context);
            btnDismiss.setText("❄️ Keep Safe (Dismiss)");
            btnDismiss.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            btnDismiss.setTextColor(0xFF0B192C);
            btnDismiss.setTypeface(AndroidUtilities.bold());
            btnDismiss.setGravity(Gravity.CENTER);
            btnDismiss.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
            GradientDrawable btnBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF00E5FF, 0xFF48CAE4}
            );
            btnBg.setCornerRadius(AndroidUtilities.dp(14));
            btnDismiss.setBackground(btnBg);
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
            buttonsLayout.addView(btnDismiss, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Secondary Settings Button
            TextView btnSettings = new TextView(context);
            btnSettings.setText("⚙️ Shield Settings");
            btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            btnSettings.setTextColor(0xFF00E5FF);
            btnSettings.setGravity(Gravity.CENTER);
            btnSettings.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(4));
            btnSettings.setOnClickListener(v -> {
                dialog.dismiss();
                if (LaunchActivity.instance != null) {
                    LaunchActivity.instance.presentFragment(new NGSettingsActivity());
                }
            });
            buttonsLayout.addView(btnSettings, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            root.addView(content, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            dialog.setContentView(root);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = (int) (AndroidUtilities.displaySize.x * 0.88f);
                int maxWidth = AndroidUtilities.dp(360);
                if (width > maxWidth) {
                    width = maxWidth;
                }
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
        } catch (Exception e) {
            FileLog.e("NGFrostShieldDialog: failed to show dialog", e);
        }
    }
}
