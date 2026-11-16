package org.telegram.ui.ActionBar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Stories.recorder.KeyboardNotifier;

public class AlertDialogDecor extends AlertDialog {

    private static final int[] ATTRS = new int[]{
            android.R.attr.windowEnterAnimation,
            android.R.attr.windowExitAnimation
    };
    private static final int DIM_DURATION = 300;

    private int resEnterAnimation;
    private int resExitAnimation;
    private View rootView;
    private View contentView;
    private View dimView;
    private OnShowListener onShowListener;
    private OnDismissListener onDismissListener;
    private boolean isDismissed = false;
    private long openDelay = 0;
    private final Runnable showRunnable = () -> {
        if (isDismissed || rootView == null || contentView == null || dimView == null) {
            return;
        }
        rootView.setVisibility(View.VISIBLE);
        dimView.setAlpha(0);
        Animation enterAnim = null;
        if (resEnterAnimation > 0) {
            try {
                enterAnim = AnimationUtils.loadAnimation(getContext(), resEnterAnimation);
            } catch (Exception ignored) {
                enterAnim = null;
            }
        }
        if (enterAnim != null) {
            contentView.startAnimation(enterAnim);
        } else {
            contentView.setAlpha(0f);
            contentView.setScaleX(0.92f);
            contentView.setScaleY(0.92f);
            contentView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
        dimView.animate().setDuration(DIM_DURATION).alpha(1f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onShowListener != null) {
                    onShowListener.onShow(AlertDialogDecor.this);
                }
            }
        }).start();
    };

    public AlertDialogDecor(Context context, int progressStyle) {
        super(context, progressStyle, null);
    }

    public AlertDialogDecor(Context context, int progressStyle, Theme.ResourcesProvider resourcesProvider) {
        super(context, progressStyle, resourcesProvider);
    }

    @Nullable
    private ViewGroup getDecorView() {
        Activity activity = getActivity(getContext());
        if (activity == null) {
            return null;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return null;
        }
        View decor = window.getDecorView();
        return decor instanceof ViewGroup ? (ViewGroup) decor : null;
    }

    private void extractAnimations() {
        resEnterAnimation = -1;
        resExitAnimation = -1;
        try {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            if (theme != null && theme.resolveAttribute(android.R.attr.windowAnimationStyle, typedValue, true) && typedValue.resourceId > 0) {
                TypedArray array = getContext().obtainStyledAttributes(typedValue.resourceId, ATTRS);
                resEnterAnimation = array.getResourceId(0, -1);
                resExitAnimation = array.getResourceId(1, -1);
                array.recycle();
            }
        } catch (Exception ignored) {
            resEnterAnimation = -1;
            resExitAnimation = -1;
        }
    }

    @Override
    protected boolean supportsNativeBlur() {
        return false;
    }

    @Override
    public void show() {
        extractAnimations();
        setDismissDialogByButtons(true);

        contentView = inflateContent(false);
        contentView.setClickable(true);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        FrameLayout container = new FrameLayout(getContext());
        container.setOnClickListener(v -> dismiss());

        dimView = new View(getContext());
        dimView.setBackgroundColor(Theme.multAlpha(Color.BLACK, params.dimAmount));
        container.addView(dimView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final FrameLayout contentWrapper = new FrameLayout(getContext());
        contentWrapper.addView(contentView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        container.addView(contentWrapper, new FrameLayout.LayoutParams(params.width, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        ViewGroup decorView = getDecorView();
        if (decorView == null) {
            super.show();
            return;
        }
        rootView = container;
        decorView.addView(rootView);
        ViewCompat.requestApplyInsets(rootView);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            final Rect rect = new Rect();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final Insets r = insets.getInsets(WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.systemBars());
                rect.set(r.left, r.top, r.right, r.bottom);
            } else {
                rect.set(insets.getStableInsetLeft(), insets.getStableInsetTop(), insets.getStableInsetRight(), insets.getStableInsetBottom());
            }
            contentWrapper.setPadding(rect.left, rect.top, rect.right, rect.bottom + AndroidUtilities.navigationBarHeight);
            contentWrapper.requestLayout();
            return insets;
        });
        rootView.setVisibility(View.INVISIBLE);

        if (openDelay == 0) {
            showRunnable.run();
        } else {
            AndroidUtilities.runOnUIThread(showRunnable, openDelay);
        }
    }

    @Override
    public void showDelayed(long delay) {
        if (isShowing()) {
            return;
        }
        openDelay = delay;
        show();
    }

    @Override
    public boolean isShowing() {
        ViewGroup decorView = getDecorView();
        return (decorView != null && rootView != null && decorView.indexOfChild(rootView) != -1 && !isDismissed) || super.isShowing();
    }

    @Override
    public void setOnShowListener(@Nullable OnShowListener listener) {
        onShowListener = listener;
    }

    @Override
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        onDismissListener = listener;
    }

    @Override
    public void dismiss() {
        ViewGroup decorView = getDecorView();
        if (decorView == null || rootView == null || decorView.indexOfChild(rootView) == -1) {
            super.dismiss();
            return;
        }
        if (isDismissed) {
            return;
        }
        isDismissed = true;
        AndroidUtilities.cancelRunOnUIThread(showRunnable);
        if (rootView.getVisibility() != View.VISIBLE) {
            decorView.removeView(rootView);
            if (onDismissListener != null) {
                onDismissListener.onDismiss(AlertDialogDecor.this);
            }
            return;
        }

        Animation animation = null;
        if (resExitAnimation > 0) {
            try {
                animation = AnimationUtils.loadAnimation(getContext(), resExitAnimation);
            } catch (Exception ignored) {
                animation = null;
            }
        }
        contentView.clearAnimation();
        if (animation != null) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    contentView.setAlpha(0f);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            contentView.startAnimation(animation);
        } else {
            contentView.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(150).start();
        }
        dimView.animate().setListener(null).cancel();
        dimView.animate().setDuration(DIM_DURATION).alpha(0f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewGroup currentDecor = getDecorView();
                if (currentDecor != null && rootView != null) {
                    currentDecor.removeView(rootView);
                }
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(AlertDialogDecor.this);
                }
            }
        }).start();
    }

    private Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return AndroidUtilities.findActivity(context);
    }

    public static class Builder extends AlertDialog.Builder {

        protected Builder(AlertDialogDecor alert) {
            super(alert);
        }

        public Builder(Context context) {
            super(context, null);
        }

        public Builder(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context, 0, resourcesProvider);
        }

        @Override
        protected AlertDialog createAlertDialog(Context context, int progressViewStyle, Theme.ResourcesProvider resourcesProvider) {
            return new AlertDialogDecor(context, progressViewStyle, resourcesProvider);
        }
    }
}
