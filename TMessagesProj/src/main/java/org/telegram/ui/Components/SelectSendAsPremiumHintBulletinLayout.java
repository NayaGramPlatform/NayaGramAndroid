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

        // Keep this group/channel Send As restriction hint readable even when the
        // surrounding Telegram theme supplies a dark undo-bulletin background.
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFFFFFFF);
        background.setCornerRadius(AndroidUtilities.dp(16));
        background.setStroke(AndroidUtilities.dp(1), 0xFFBAE6FD);
        setCustomBackground(background);

        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.msg_premium_prolfilestar));
        imageView.setColorFilter(new PorterDuffColorFilter(0xFF0284C7, PorterDuff.Mode.SRC_IN));
        textView.setTextColor(0xFF0F172A);
        textView.setLinkTextColor(0xFF0284C7);
        textView.setText(AndroidUtilities.replaceTags(LocaleController.getString(channels ? R.string.SelectSendAsPeerPremiumHint : R.string.SelectSendAsPeerPremiumHint)));

        Bulletin.UndoButton button = new Bulletin.UndoButton(context, true, resourcesProvider);
        button.setText(LocaleController.getString(R.string.SelectSendAsPeerPremiumOpen));
        if (button.undoTextView != null) button.undoTextView.setTextColor(0xFF0284C7);
        button.setUndoAction(callback);
        setButton(button);
    }
}
