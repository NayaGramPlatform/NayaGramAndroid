package org.nayagram.platform.monetization;

import android.content.Context;

/**
 * Provider contract for optional monetization networks.
 *
 * Implementations must fail closed when disabled or when an ad is not ready.
 * The callback must only run after an ad impression is actually recorded.
 */
public interface INGMonetizationProvider {
    boolean isMonetizationEnabled();
    void initialize(Context context);
    boolean showInterstitialIfAvailable(Context context, Runnable onImpression);
}
