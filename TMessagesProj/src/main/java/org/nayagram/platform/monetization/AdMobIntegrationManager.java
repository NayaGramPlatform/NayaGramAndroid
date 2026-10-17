package org.nayagram.platform.monetization;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import org.telegram.messenger.NayaGramAdsConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Google Mobile Ads adapter.
 *
 * Ads remain disabled by the central policy until a production account and
 * ad-unit IDs are configured. Consent is refreshed before the SDK requests ads.
 */
public final class AdMobIntegrationManager implements INGMonetizationProvider {

    private static final String TAG = "NayaGramAdMob";
    private static final long RETRY_DELAY_MS = 60_000L;

    private static volatile AdMobIntegrationManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean initializationStarted = new AtomicBoolean(false);
    private volatile Context appContext;
    private volatile boolean sdkInitialized;
    private volatile InterstitialAd interstitialAd;
    private final List<WeakReference<FrameLayout>> pendingBannerContainers =
        Collections.synchronizedList(new ArrayList<>());

    private AdMobIntegrationManager() { }

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
        return NayaGramAdsConfig.ENABLE_ADS && sdkInitialized;
    }

    @Override
    public void initialize(Context context) {
        if (!NayaGramAdsConfig.ENABLE_ADS || context == null) {
            Log.i(TAG, "Ads are disabled by policy; SDK initialization skipped.");
            return;
        }
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Consent must be handled from an Activity; deferring ad initialization.");
            return;
        }
        if (!initializationStarted.compareAndSet(false, true)) {
            return;
        }

        appContext = context.getApplicationContext();
        Activity activity = (Activity) context;
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters parameters = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                if (formError != null) {
                    Log.w(TAG, "Consent form did not complete: " + formError.getMessage());
                }
                initializeIfConsentAllows(consentInformation);
            }),
            requestError -> {
                Log.w(TAG, "Consent info update failed: " + requestError.getMessage());
                // A previously saved consent state may still permit requests.
                initializeIfConsentAllows(consentInformation);
            }
        );
    }

    private void initializeIfConsentAllows(ConsentInformation consentInformation) {
        if (!NayaGramAdsConfig.ENABLE_ADS || !consentInformation.canRequestAds()) {
            Log.i(TAG, "Ad requests are blocked until consent requirements are satisfied.");
            return;
        }
        final Context context = appContext;
        if (context == null) return;

        new Thread(() -> {
            try {
                MobileAds.initialize(context, initializationStatus -> {
                    sdkInitialized = true;
                    loadInterstitial();
                    attachPendingBanners();
                    Log.i(TAG, "Google Mobile Ads SDK initialized.");
                });
            } catch (RuntimeException error) {
                initializationStarted.set(false);
                Log.e(TAG, "Google Mobile Ads initialization failed.", error);
            }
        }, "NayaGram-AdMob-Init").start();
    }

    private void loadInterstitial() {
        final Context context = appContext;
        if (context == null || !NayaGramAdsConfig.ENABLE_ADS || !sdkInitialized) {
            return;
        }
        try {
            InterstitialAd.load(
                context,
                NayaGramAdsConfig.INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Interstitial ad loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitialAd = null;
                        Log.w(TAG, "Interstitial load failed: " + error.getMessage());
                        if (NayaGramAdsConfig.ENABLE_ADS) {
                            mainHandler.postDelayed(AdMobIntegrationManager.this::loadInterstitial, RETRY_DELAY_MS);
                        }
                    }
                }
            );
        } catch (RuntimeException error) {
            interstitialAd = null;
            Log.e(TAG, "Could not request an interstitial ad.", error);
        }
    }


    /**
     * Attach a banner to an explicitly supplied home/dialogs-screen container.
     * Containers are queued while UMP consent or SDK initialization is pending.
     */
    public void attachBanner(Activity activity, FrameLayout container) {
        if (!NayaGramAdsConfig.shouldShowBanner() || activity == null || container == null) {
            return;
        }
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) {
            return;
        }
        if (!sdkInitialized) {
            pendingBannerContainers.add(new WeakReference<>(container));
            return;
        }
        mainHandler.post(() -> attachBannerNow(container));
    }

    private void attachPendingBanners() {
        synchronized (pendingBannerContainers) {
            for (WeakReference<FrameLayout> reference : pendingBannerContainers) {
                FrameLayout container = reference.get();
                if (container != null) {
                    mainHandler.post(() -> attachBannerNow(container));
                }
            }
            pendingBannerContainers.clear();
        }
    }

    private void attachBannerNow(FrameLayout container) {
        if (!NayaGramAdsConfig.shouldShowBanner() || container == null) return;
        if (container.getChildCount() > 0) return;
        Context context = container.getContext();
        try {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int widthDp = Math.max(1, (int) (metrics.widthPixels / metrics.density));
            AdView banner = new AdView(context);
            banner.setAdUnitId(NayaGramAdsConfig.BANNER_AD_UNIT_ID);
            banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp));
            banner.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    Log.w(TAG, "Dialogs banner failed to load: " + error.getMessage());
                    container.removeView(banner);
                    container.setVisibility(View.GONE);
                }
            });
            container.setVisibility(View.VISIBLE);
            container.addView(banner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ));
            banner.loadAd(new AdRequest.Builder().build());
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not attach dialogs banner.", error);
            container.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean showInterstitialIfAvailable(Context context, Runnable onImpression) {
        if (!(context instanceof Activity) || !isMonetizationEnabled()) {
            return false;
        }
        Activity activity = (Activity) context;
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) {
            return false;
        }

        final InterstitialAd ad = interstitialAd;
        if (ad == null) {
            return false;
        }
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdImpression() {
                if (onImpression != null) {
                    onImpression.run();
                }
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitial();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.w(TAG, "Interstitial could not be shown: " + error.getMessage());
                loadInterstitial();
            }
        });

        try {
            ad.show(activity);
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "Interstitial show failed.", error);
            loadInterstitial();
            return false;
        }
    }
}
