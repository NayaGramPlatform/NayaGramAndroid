package org.telegram.ui.ActionBar;

import android.content.Context;

public class DarkAlertDialog extends AlertDialog {

	public DarkAlertDialog(Context context, int progressStyle) {
		super(context, progressStyle);
	}

	@Override
	protected int getThemedColor(int key) {
        // Respect the active theme; hard-coded dark surfaces made dialogs unreadable in light mode.
        return super.getThemedColor(key);
	}

	public static class Builder extends AlertDialog.Builder {

		public Builder(Context context) {
			super(new DarkAlertDialog(context, 0));
		}

		public Builder(Context context, int progressViewStyle) {
			super(new DarkAlertDialog(context, progressViewStyle));
		}
	}
}
