package org.nayagram.platform.monetization;

import android.content.Context;

/**
 * Clean monetization abstraction interface.
 * Currently keeps ads completely disabled to comply with Play Store ad declarations
 * while providing an extensible plug-and-play architecture for future activation.
 */
public interface INGMonetizationProvider {
    boolean isMonetizationEnabled();
    void initialize(Context context);
    void showInterstitialIfAvailable(Context context);
}
