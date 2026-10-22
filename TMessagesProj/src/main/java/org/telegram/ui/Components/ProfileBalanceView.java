package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.ui.ActionBar.Theme.multAlpha;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Stars.StarsController;

/**
 * Pill-shaped Balance badge on user profile (e.g. "Balance: 💎 5,092.49 >").
 * Styled to match ProfileMusicView with glassmorphism blur, diamond icon, and chevron.
 */
public class ProfileBalanceView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diamondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final ButtonBounce bounce = new ButtonBounce(this);

    private String balanceText = "Balance: 💎 0 >";
    private String amountStr = "0";
    private float currentHeight;
    private int currentAccount = UserConfig.selectedAccount;
    private boolean isSideBySide;
    private boolean isLeft;
    private Drawable chevronDrawable;
    private int peerColor = 0xFF0284C7;

    public ProfileBalanceView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        backgroundPaint.setColor(0xE60F172A);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(0x33FFFFFF);

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dp(12.5f));
        textPaint.setTypeface(AndroidUtilities.bold());

        diamondPaint.setColor(0xFF00D4FF);
        chevronDrawable = ContextCompat.getDrawable(context, R.drawable.msg_arrowright);
        if (chevronDrawable != null) {
            chevronDrawable = chevronDrawable.mutate();
            chevronDrawable.setColorFilter(new PorterDuffColorFilter(0xB3FFFFFF, PorterDuff.Mode.SRC_IN));
        }
    }

    public void setAccount(int account) {
        this.currentAccount = account;
    }

    public void setBalance(String amount) {
        this.amountStr = amount;
        this.balanceText = "Balance: 💎 " + amount;
        invalidate();
    }

    public void setPeerColor(int color) {
        this.peerColor = color;
        invalidate();
    }

    public void setSideBySide(boolean sideBySide, boolean left) {
        this.isSideBySide = sideBySide;
        this.isLeft = left;
        invalidate();
    }

    public void updatePosition(float y, float newHeight) {
        this.currentHeight = newHeight;
        setTranslationY(y - dp(12));
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float alpha = Utilities.clamp01(currentHeight / dp(21));
        if (alpha <= 0) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            bounce.setPressed(rect.contains(event.getX(), event.getY()));
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && bounce.isPressed()) {
            if (!rect.contains(event.getX(), event.getY())) {
                bounce.setPressed(false);
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP && bounce.isPressed()) {
            bounce.setPressed(false);
            callOnClick();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            bounce.setPressed(false);
        }
        return bounce.isPressed();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final float alpha = Utilities.clamp01(currentHeight / dp(21));
        if (alpha <= 0) return;

        final float scale = bounce.getScale(0.02f);
        final float textWidth = textPaint.measureText(balanceText);
        final float chevronWidth = dp(10);
        final float pillWidth = textWidth + chevronWidth + dp(24);
        final float pillHeight = dp(24) * alpha;

        float centerX;
        if (isSideBySide) {
            if (isLeft) {
                centerX = getWidth() * 0.28f;
            } else {
                centerX = getWidth() * 0.72f;
            }
        } else {
            centerX = getWidth() / 2f;
        }

        final float top = (getHeight() - pillHeight) / 2f;
        rect.set(centerX - pillWidth / 2f, top, centerX + pillWidth / 2f, top + pillHeight);

        canvas.save();
        canvas.scale(scale, scale, centerX, getHeight() / 2f);

        // Glassmorphism background pill
        backgroundPaint.setColor(Theme.isCurrentThemeDark() ? 0xCC111827 : 0xDD1E293B);
        strokePaint.setShader(new LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[]{0x6600D4FF, 0x337C3AED}, null, Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, backgroundPaint);
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, strokePaint);

        // Text & Chevron
        float textX = rect.left + dp(10);
        float textY = rect.centerY() + dp(4);
        textPaint.setAlpha((int) (255 * alpha));
        canvas.drawText(balanceText, textX, textY, textPaint);

        if (chevronDrawable != null) {
            int chSize = dp(10);
            int chLeft = (int) (rect.right - dp(14));
            int chTop = (int) (rect.centerY() - chSize / 2f);
            chevronDrawable.setBounds(chLeft, chTop, chLeft + chSize, chTop + chSize);
            chevronDrawable.setAlpha((int) (200 * alpha));
            chevronDrawable.draw(canvas);
        }

        canvas.restore();
    }
}
