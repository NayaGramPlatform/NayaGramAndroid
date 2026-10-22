package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class SelectSendAsPremiumHintBulletinLayout extends Bulletin.MultiLineLayout {

    public SelectSendAsPremiumHintBulletinLayout(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, boolean channels, Runnable callback) {
        super(context, resourcesProvider);

        // Theme-aware Premium hint with a NayaGram accent, readable in light and dark modes.
        int surface = Theme.getColor(Theme.key_dialogBackground, resourcesProvider);
        int primaryText = Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider);
        int accent = Theme.getColor(Theme.key_dialogTextBlue, resourcesProvider);
        GradientDrawable background = new GradientDrawable();
        background.setColor(surface);
        background.setCornerRadius(AndroidUtilities.dp(16));
        background.setStroke(AndroidUtilities.dp(1), accent);
        setCustomBackground(background);

        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.msg_premium_prolfilestar));
        imageView.setColorFilter(new PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN));
        textView.setTextColor(primaryText);
        textView.setLinkTextColor(accent);
        textView.setText(AndroidUtilities.replaceTags(LocaleController.getString(channels ? R.string.SelectSendAsPeerPremiumHint : R.string.SelectSendAsPeerPremiumHint)));

        Bulletin.UndoButton button = new Bulletin.UndoButton(context, true, resourcesProvider);
        button.setText(LocaleController.getString(R.string.SelectSendAsPeerPremiumOpen));
        if (button.undoTextView != null) button.undoTextView.setTextColor(accent);
        button.setUndoAction(callback);
        setButton(button);
    }
}
