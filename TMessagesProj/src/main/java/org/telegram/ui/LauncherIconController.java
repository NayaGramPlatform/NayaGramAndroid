package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

public class LauncherIconController {

    public static void tryFixLauncherIconIfNeeded() {
        try {
            for (LauncherIcon icon : LauncherIcon.values()) {
                if (isEnabled(icon)) {
                    return;
                }
            }
            setIcon(LauncherIcon.DEFAULT);
        } catch (Throwable t) {
            FileLog.e("LauncherIconController.tryFixLauncherIconIfNeeded error", t);
        }
    }

    public static boolean isEnabled(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) {
                return icon == LauncherIcon.DEFAULT;
            }
            int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
            return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || 
                   (i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT);
        } catch (Throwable t) {
            return icon == LauncherIcon.DEFAULT;
        }
    }

    public static void setIcon(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) {
                return;
            }
            PackageManager pm = ctx.getPackageManager();
            for (LauncherIcon i : LauncherIcon.values()) {
                try {
                    pm.setComponentEnabledSetting(
                            i.getComponentName(ctx),
                            i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                    );
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            FileLog.e("LauncherIconController.setIcon error", t);
        }
    }

    public enum LauncherIcon {
        DEFAULT("NGSky", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconDefault),
        VINTAGE("NGBlue", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("NGPurple", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, true),
        TURBO("NGNeon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, false),
        NOX("NGCrystal", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, false);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.nayagram.platform." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
