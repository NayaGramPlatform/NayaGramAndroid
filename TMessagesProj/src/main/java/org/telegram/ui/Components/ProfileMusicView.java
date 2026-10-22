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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

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
import org.telegram.ui.ProfileActivity;

public class ProfileMusicView extends View {

    private final Theme.ResourcesProvider resourcesProvider;

    private final PorterDuffColorFilter filterColorWhite = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    private final PorterDuffColorFilter filterColorBlack = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

    private Text author, title;
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint equalizerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradientOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();
    private final Drawable icon;

    private final RectF rect = new RectF();
    private final RectF barRect = new RectF();
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    private int textColor = Color.WHITE;
    private int subtitleColor = Color.WHITE;
    private float parentExpanded;
    private int backgroundColor;
    private boolean withShadows;

    public void setColor(MessagesController.PeerColor peerColor) {
        int color1, color2;
        if (peerColor == null) {
            color1 = color2 = Theme.getColor(Theme.key_actionBarDefault, resourcesProvider);
        } else {
            color1 = peerColor.getBgColor1(Theme.isCurrentThemeDark());
            color2 = peerColor.getBgColor2(Theme.isCurrentThemeDark());
        }
        if (peerColor == null) {
            backgroundColor = Theme.isCurrentThemeDark()
                    ? 0xFF0F172A
                    : Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider);
            withShadows = true;
        } else {
            backgroundColor = Theme.adaptHSV(ColorUtils.blendARGB(color1, color2, .15f), +.04f, -.09f);
            withShadows = false;
        }
        backgroundPaint.setColor(backgroundColor);
        checkTextColor();
    }

    private void checkTextColor() {
        final boolean isDark = Theme.isCurrentThemeDark();
        final boolean useBlackText = !isDark && parentExpanded < 0.8f && AndroidUtilities.computePerceivedBrightness(backgroundColor) > 0.72f;
        textColor = useBlackText ? Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider) : Color.WHITE;
        subtitleColor = useBlackText ? 0xFF6D28D9 : 0xFFE0F7FA;
        icon.setColorFilter(new PorterDuffColorFilter(0xFF00D4FF, PorterDuff.Mode.SRC_IN));
        iconPaint.setColor(textColor);
        iconBadgePaint.setColor(isDark ? 0x3300D4FF : 0x2200B4D8);
        arrowPaint.setColor(0xFF00D4FF);
        invalidate();
    }

    public void setParentExpanded(float expanded) {
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

    private ProfileActivity.AvatarImageView avatarView;
    private boolean ignoreRect = false;
    private RenderNode renderNode;
    private float renderNodeScale;
    private float renderNodeTranslateY;

    public void drawingBlur(boolean drawing) {
        if (ignoreRect != drawing || renderNode != null) {
            ignoreRect = drawing;
            renderNode = null;
            avatarView = null;
            invalidate();
        }
    }

    public void drawingBlur(RenderNode renderNode, ProfileActivity.AvatarImageView avatarView, float scale, float dy) {
        this.ignoreRect = false;
        this.renderNode = renderNode;
        this.avatarView = avatarView;
        this.renderNodeScale = scale;
        this.renderNodeTranslateY = dy;
        invalidate();
    }

    private float currentHeight;

    public void updatePosition(float y, float newHeight) {
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

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (this.author == null || this.title == null) return;

        final float alpha = Utilities.clamp01((currentHeight) / dp(21));
        final float scale = bounce.getScale(0.02f);
        if (alpha <= 0) return;

        final int padding = dp(12);
        final int maxWidth = getWidth() - padding * 2;
        final float eqWidth = dp(14);

        this.author.ellipsize((maxWidth - dp(52)) / 2f);
        this.title.ellipsize(maxWidth - this.author.getWidth() - dp(52));

        final float width = dp(20f) + this.author.getWidth() + this.title.getWidth() + eqWidth + dp(10);
        final float containerWidth = dp(18) + width;

        canvas.save();
        canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);

        final float pillHeight = dp(22) * alpha;
        final float top = (getHeight() - pillHeight) / 2f;
        rect.set(
            (getWidth() - containerWidth) / 2f,
            top,
            (getWidth() + containerWidth) / 2f,
            top + pillHeight
        );
        if (withShadows && SharedConfig.shadowsInSections) {
            backgroundPaint.setShadowLayer(dpf2(3), 0, dpf2(1f), multAlpha(0x2200B4D8, alpha));
            strokePaint.setShadowLayer(dpf2(0.5f), 0, 0, multAlpha(0x18000000, alpha));
            strokePaint.setColor(0);
        } else {
            backgroundPaint.setShadowLayer(0, 0, 0, 0);
        }
        int wasAlpha = backgroundPaint.getAlpha();
        backgroundPaint.setAlpha((int) (wasAlpha * alpha));
        if (withShadows && SharedConfig.shadowsInSections) {
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, strokePaint);
        }
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, backgroundPaint);

        gradientOverlayPaint.setShader(new LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[]{multAlpha(0x2800D4FF, alpha), multAlpha(0x227C3AED, alpha), multAlpha(0x2410B981, alpha)},
            null,
            Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, gradientOverlayPaint);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpf2(1.3f));
        strokePaint.setColor(Theme.isCurrentThemeDark() ? 0x8000D4FF : 0x5500D4FF);
        strokePaint.setShader(new LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            new int[]{multAlpha(0xCC00D4FF, alpha), multAlpha(0xAA7C3AED, alpha), multAlpha(0xCC10B981, alpha)},
            null,
            Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, strokePaint);
        strokePaint.setShader(null);
        backgroundPaint.setAlpha(wasAlpha);

        clipPath.rewind();
        clipPath.addRoundRect(rect, rect.height() / 2f, rect.height() / 2f, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);
        if (!ignoreRect && renderNode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canvas.isHardwareAccelerated()) {
            canvas.save();
            canvas.translate(0f, renderNodeTranslateY);
            canvas.scale(renderNodeScale, renderNodeScale);
            canvas.drawRenderNode(renderNode);
            canvas.restore();
        }

        canvas.translate((getWidth() - width) / 2f, 0);

        final float cy = getHeight() / 2f;
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

        // Live animated 3-bar NayaGram equalizer (Cyan, Purple, Emerald)
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

        canvas.restore();
        canvas.restore();

        if (alpha > 0.05f && getVisibility() == VISIBLE) {
            postInvalidateOnAnimation();
        }
    }
}
