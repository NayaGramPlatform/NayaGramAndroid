package org.nayagram.platform.monetization;

import android.content.Context;

public class AdMobIntegrationManager implements INGMonetizationProvider {

    private static volatile AdMobIntegrationManager instance;

    public static AdMobIntegrationManager getInstance() {
        if (instance == null) {
            synchronized (AdMobIntegrationManager.class) {
                if (instance == null) {
                    instance = new AdMobIntegrationManager();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean isMonetizationEnabled() {
        // Ads remain OFF by policy; toggle here when ready to activate future ad networks
        return false;
    }

    @Override
    public void initialize(Context context) {
        // Safe stub: No third-party SDK initialized
    }

    @Override
    public void showInterstitialIfAvailable(Context context) {
        // Safe stub: No ads shown
    }
}
