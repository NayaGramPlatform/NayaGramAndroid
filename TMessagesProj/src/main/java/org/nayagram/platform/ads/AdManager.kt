package org.nayagram.platform.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import org.nayagram.platform.monetization.INGMonetizationProvider

/**
 * NayaGram Monetization & Sponsor Manager (Kotlin)
 * ------------------------------------------------
 * Clean, modular architecture for future monetization and sponsorships.
 * Fully compliant with Google Play Store & Telegram Platform Policies:
 *  - Master switch: disabled by default (isEnabled = false) for launch & initial months.
 *  - Extensible provider interface (INGMonetizationProvider) ready to plug in future ad networks
 *    or direct sponsor banners without touching core messaging code.
 *  - Rate-limited with 15-minute cooldown and user action threshold.
 */
object AdManager {

    private const val TAG = "NayaGramMonetization"

    // =========================================================================
    // 1. Master Configuration & Extensibility
    // =========================================================================
    @JvmStatic
    @Volatile
    var isEnabled: Boolean = false

    @JvmStatic
    @Volatile
    var activeProvider: INGMonetizationProvider? = null

    // Display interval: minimum 15 minutes between promotional/monetization events
    const val DISPLAY_INTERVAL_MINUTES: Long = 15L
    const val DISPLAY_INTERVAL_MS: Long = DISPLAY_INTERVAL_MINUTES * 60 * 1000L

    // Minimum user actions before eligible
    const val MINIMUM_ACTIONS_BEFORE_AD: Int = 10

    // =========================================================================
    // 2. State Tracking
    // =========================================================================
    private var lastAdShownTimestamp: Long = 0L
    private var actionCounter: Int = 0

    @JvmStatic
    @Synchronized
    fun recordUserAction() {
        actionCounter++
    }

    @JvmStatic
    @Synchronized
    fun canShowInterstitial(): Boolean {
        if (!isEnabled) {
            return false
        }
        val provider = activeProvider
        if (provider == null || !provider.isMonetizationEnabled) {
            return false
        }
        if (actionCounter < MINIMUM_ACTIONS_BEFORE_AD) {
            return false
        }
        val currentTime = SystemClock.elapsedRealtime()
        val timeSinceLastAd = currentTime - lastAdShownTimestamp
        return timeSinceLastAd >= DISPLAY_INTERVAL_MS
    }

    @JvmStatic
    @Synchronized
    fun onAdDisplayed() {
        lastAdShownTimestamp = SystemClock.elapsedRealtime()
        actionCounter = 0
        Log.i(TAG, "Monetization display event triggered. Cooldown active.")
    }

    @JvmStatic
    fun showInterstitialIfReady(activity: Activity): Boolean {
        if (canShowInterstitial()) {
            val provider = activeProvider
            if (provider != null && provider.isMonetizationEnabled) {
                onAdDisplayed()
                provider.showInterstitialIfAvailable(activity)
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun registerProvider(provider: INGMonetizationProvider) {
        activeProvider = provider
        Log.i(TAG, "Monetization provider registered: " + provider.javaClass.simpleName)
    }

    @JvmStatic
    @Synchronized
    fun resetTimer() {
        lastAdShownTimestamp = SystemClock.elapsedRealtime()
        actionCounter = 0
    }
}
