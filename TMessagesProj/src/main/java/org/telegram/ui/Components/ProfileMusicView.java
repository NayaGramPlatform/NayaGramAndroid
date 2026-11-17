package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.ActionBar.Theme.multAlpha;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

public class ProfileMusicView extends View {

    private final Theme.ResourcesProvider resourcesProvider;
    private final PorterDuffColorFilter filterColorWhite = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    private final PorterDuffColorFilter filterColorBlack = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

    private Text author, title;
    private ProfileMusicView trailingView;
    private ProfileMusicView leadingView;

    // The two pills share a centered row and the same profile/blur animation state.
    public void setTrailingView(ProfileMusicView view) {
        trailingView = view;
        view.leadingView = this;
        invalidate();
        view.invalidate();
    }

    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint equalizerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradientOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();
    private final Drawable icon;

    private final RectF rect = new RectF();
    private final RectF barRect = new RectF();

    public void getButtonBoundsOnScreen(RectF bounds) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        bounds.set(rect);
        bounds.offset(location[0], location[1]);
    }

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();

    private final ButtonBounce bounce = new ButtonBounce(this);

    public ProfileMusicView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        icon = context.getResources().getDrawable(R.drawable.files_music).mutate();

        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
        arrowPath.moveTo(0, -dpf2(3.33f));
        arrowPath.lineTo(dpf2(3.16f), 0);
        arrowPath.lineTo(0, dpf2(3.33f));

        equalizerPaint.setStyle(Paint.Style.FILL);

        setColor(null);
        setText("Author", " - Title");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(
                dp(37),
                MeasureSpec.EXACTLY
            )
        );
    }

    protected int textColor = Color.WHITE;
    private int subtitleColor = Color.WHITE;
    protected boolean hasProfileColor;
    private float parentExpanded;
    private int buttonColor;
    private boolean hasColorById;
    private RadialGradient radialGradient;
    private float radialGradientWidth;
    private final Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();

    public void setColor(MessagesController.PeerColor peerColor) {
        hasProfileColor = peerColor != null;
        if (trailingView != null) trailingView.setColor(peerColor);
        invalidate();
    }

    // Same color as ProfileActionsView buttons.
    public void setButtonColor(int color, boolean hasColorById) {
        if (trailingView != null) trailingView.setButtonColor(color, hasColorById);
        if (buttonColor == color && this.hasColorById == hasColorById) return;
        buttonColor = color;
        this.hasColorById = hasColorById;
        radialGradient = null;
        backgroundPaint.setColor(color);
        checkTextColor();
    }

    private boolean isButtonColorLight() {
        return AndroidUtilities.computePerceivedBrightness(buttonColor) > 0.72f;
    }

    private void checkTextColor() {
        final boolean isDark = Theme.isCurrentThemeDark();
        final boolean useBlackText = !isDark && parentExpanded < 0.8f && isButtonColorLight();
        textColor = useBlackText ? Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider) : Color.WHITE;
        subtitleColor = useBlackText ? 0xFF6D28D9 : 0xFFE0F7FA;
        icon.setColorFilter(new PorterDuffColorFilter(0xFF00D4FF, PorterDuff.Mode.SRC_IN));
        iconPaint.setColor(textColor);
        iconBadgePaint.setColor(isDark ? 0x3300D4FF : 0x2200B4D8);
        arrowPaint.setColor(0xFF00D4FF);
        invalidate();
    }

    public void setParentExpanded(float expanded) {
        if (trailingView != null) trailingView.setParentExpanded(expanded);
        if (parentExpanded != expanded) {
            parentExpanded = expanded;
            checkTextColor();
            invalidate();
        }
    }



    public void setMusicDocument(TLRPC.Document document) {
        CharSequence author = getAuthor(document);
        CharSequence title = getTitle(document);
        if (TextUtils.isEmpty(author)) {
            if (TextUtils.isEmpty(title)) {
                author = getString(R.string.AudioUnknownArtist);
                title = " - " + getString(R.string.AudioUnknownTitle);
            } else {
                author = "";
            }
        } else if (!TextUtils.isEmpty(title)) {
            title = " • " + title;
        } else {
            title = "";
        }
        setText(author, title);
    }

    public static CharSequence getTitle(TLRPC.Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                String title = attribute.title;
                if (title == null || title.length() == 0) {
                    title = FileLoader.getDocumentFileName(document);
                }
                return title;
            }
        }
        String fileName = FileLoader.getDocumentFileName(document);
        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }
//        return getString(R.string.AudioUnknownTitle);
        return null;
    }

    public static CharSequence getAuthor(TLRPC.Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                if (!attribute.voice) {
                    return attribute.performer;
                }
            }
        }
        return null;
    }

    public void setText(CharSequence author, CharSequence title) {
        this.author = new Text(author, 12.2f, AndroidUtilities.bold());
        this.title = new Text(title, 12f);

        setContentDescription(getString(R.string.AccDescrProfileMusic) + " " + author + " — " + title);
    }

    public void setSideBySide(boolean sideBySide, boolean left) {
        invalidate();
    }

    /** The pill's glass, like the action buttons'; without it the pill is filled with their color. */
    @Nullable
    private BlurredBackgroundDrawable glass;
    private float glassRadius;

    /** The pill on glass, see {@link ProfileButtonsGlass#actionsFactory}. */
    public void setGlassFactory(@Nullable BlurredBackgroundDrawableViewFactory factory) {
        if (trailingView != null) trailingView.setGlassFactory(factory);
        glass = factory != null ? factory.create(this, ProfileButtonsGlass.NO_COLORS) : null;
        glassRadius = 0;
        invalidate();
    }

    private float currentHeight;

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (trailingView != null) trailingView.setAlpha(alpha);
    }

    protected boolean isOnProfilePhoto() {
        return parentExpanded >= 0.8f;
    }

    public void updatePosition(float y, float newHeight) {
        if (trailingView != null) trailingView.updatePosition(y, newHeight);
        currentHeight = newHeight;
        setTranslationY(y - dp(12));
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float alpha = Utilities.clamp01((currentHeight) / dp(21));
        if (alpha <= 0) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            bounce.setPressed(rect.contains(event.getX(), event.getY()));
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && bounce.isPressed()) {
            if (!rect.contains(event.getX(), event.getY())) {
                bounce.setPressed(false);
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            bounce.setPressed(false);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (bounce.isPressed()) {
                performClick();
            }
            bounce.setPressed(false);
        }
        return bounce.isPressed();
    }

    protected float getContentWidth(float maxWidth) {
        final float eqWidth = dp(14);
        final float textWidth = Math.max(0, maxWidth - dp(18) - dp(20f) - eqWidth - dp(10));
        this.author.ellipsize(textWidth / 2f);
        this.title.ellipsize(Math.max(0, textWidth - this.author.getWidth()));
        return dp(20f) + this.author.getWidth() + this.title.getWidth() + eqWidth + dp(10);
    }

    protected void drawContent(Canvas canvas, float alpha) {
        final float cy = getHeight() / 2f;
        final float eqWidth = dp(14);
        final float badgeRadius = dp(8f);
        canvas.drawCircle(badgeRadius, cy, badgeRadius, iconBadgePaint);

        final int iconSz = dp(11);
        icon.setBounds((int) (badgeRadius - iconSz / 2f), (int) (cy - iconSz / 2f), (int) (badgeRadius + iconSz / 2f), (int) (cy + iconSz / 2f));
        icon.draw(canvas);

        canvas.translate(dp(20f), 0);
        this.author.draw(canvas, 0, cy, textColor, alpha);
        canvas.translate(this.author.getWidth(), 0);
        this.title.draw(canvas, 0, cy, subtitleColor, 0.92f * alpha);
        canvas.translate(this.title.getWidth() + dp(4), 0);

        final long now = SystemClock.elapsedRealtime();
        final int[] eqColors = new int[]{0xFF00D4FF, 0xFF7C3AED, 0xFF10B981};
        final float barW = dpf2(2.2f);
        final float barGap = dpf2(1.8f);
        final float maxBarH = dp(10f) * alpha;
        final float minBarH = dp(3.2f) * alpha;
        for (int i = 0; i < 3; i++) {
            float wave = (float) (0.5f + 0.5f * Math.sin((now / 180.0) + i * 1.35));
            float bh = lerp(minBarH, maxBarH, wave);
            float bx = i * (barW + barGap);
            equalizerPaint.setColor(multAlpha(eqColors[i], alpha));
            barRect.set(bx, cy - bh / 2f, bx + barW, cy + bh / 2f);
            canvas.drawRoundRect(barRect, barW / 2f, barW / 2f, equalizerPaint);
        }
        canvas.translate(eqWidth, 0);

        arrowPaint.setStrokeWidth(dpf2(1.25f));
        canvas.translate(dpf2(3.2f), cy);
        canvas.drawPath(arrowPath, arrowPaint);

        if (alpha > 0.05f && getVisibility() == VISIBLE) {
            postInvalidateOnAnimation();
        }
    }

    private boolean hasVisibleCompanion() {
        ProfileMusicView companion = trailingView != null ? trailingView : leadingView;
        return companion != null && companion.getVisibility() == VISIBLE;
    }

    private float getPillWidth(float maxWidth) {
        return dp(16) + getContentWidth(maxWidth);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (this.author == null || this.title == null) return;

        final float alpha = Utilities.clamp01((currentHeight) / dp(21));
        final float scale = bounce.getScale(0.02f);
        if (alpha <= 0) {
            if (glass != null) {
                glass.setAlpha(0);
            }
            return;
        }

        final int padding = dp(12);
        final int maxWidth = getWidth() - padding * 2;

        float containerWidth;
        float left;
        if (hasVisibleCompanion()) {
            ProfileMusicView leading = leadingView != null ? leadingView : this;
            ProfileMusicView trailing = trailingView != null ? trailingView : this;
            // Reserve the balance first so long song titles cannot push it offscreen.
            float trailingWidth = trailing.getPillWidth(maxWidth / 2f);
            float leadingWidth = leading.getPillWidth(Math.max(0, maxWidth - trailingWidth - dp(6)));
            left = (getWidth() - leadingWidth - dp(6) - trailingWidth) / 2f;
            containerWidth = leadingView != null ? trailingWidth : leadingWidth;
            if (leadingView != null) left += leadingWidth + dp(6);
        } else {
            containerWidth = getPillWidth(maxWidth);
            left = (getWidth() - containerWidth) / 2f;
        }

        rect.set(left, dp(10), left + containerWidth, dp(10) + dp(17) * alpha);
        canvas.save();
        canvas.scale(scale, scale, rect.centerX(), getHeight() / 2f);

        if (glass != null) {
            final float radius = rect.height() / 2f;
            if (glassRadius != radius) {
                glassRadius = radius;
                glass.setRadius(radius);
            }
            glass.setBounds((int) rect.left, (int) rect.top, (int) Math.ceil(rect.right), (int) Math.ceil(rect.bottom));
            glass.setAlpha((int) (0xFF * alpha));
            glass.draw(canvas);
        } else {
            final float bgAlpha = hasColorById ? 0.1f : 1f;
            backgroundPaint.setShadowLayer(0, 0, 0, 0);
            int wasAlpha = backgroundPaint.getAlpha();
            backgroundPaint.setAlpha((int) (wasAlpha * alpha * bgAlpha));
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, backgroundPaint);
            backgroundPaint.setAlpha(wasAlpha);
            if (hasColorById && buttonColor != 0) {
                if (radialGradient == null || radialGradientWidth != rect.width()) {
                    radialGradientWidth = rect.width();
                    radialGradient = new RadialGradient(
                        rect.width() / 2f, dp(17) / 2f, rect.width() * 0.65f,
                        Theme.multAlpha(buttonColor, 0.8f), buttonColor,
                        Shader.TileMode.CLAMP
                    );
                    shaderPaint.setShader(radialGradient);
                }
                matrix.setTranslate(rect.left, rect.top);
                radialGradient.setLocalMatrix(matrix);
                shaderPaint.setAlpha((int) (0xFF * alpha));
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, shaderPaint);
            }
        }

        gradientOverlayPaint.setShader(new LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[]{multAlpha(0x2800D4FF, alpha), multAlpha(0x227C3AED, alpha), multAlpha(0x2410B981, alpha)},
            null,
            Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, gradientOverlayPaint);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpf2(1.2f));
        strokePaint.setShader(new LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[]{multAlpha(0xCC00D4FF, alpha), multAlpha(0xAA7C3AED, alpha), multAlpha(0xCC10B981, alpha)},
            null,
            Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, strokePaint);
        strokePaint.setShader(null);

        clipPath.rewind();
        clipPath.addRoundRect(rect, rect.height() / 2f, rect.height() / 2f, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);
        canvas.translate(rect.left + dp(8), 0);
        drawContent(canvas, alpha);

        canvas.restore();

        canvas.restore();
    }
}
