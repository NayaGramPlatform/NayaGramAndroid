package org.nayagram.platform.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log

/**
 * NayaGram Official AdManager (Kotlin)
 * -------------------------------------
 * Handles display logic for Google AdMob Interstitials.
 * Features:
 *  - Configurable 15-minute display interval (respects user experience)
 *  - Master switch: disabled by default (isEnabled = false) for the initial 3-5 months launch
 *  - Clean, thread-safe, modular Kotlin implementation
 */
object AdManager {

    private const val TAG = "NayaGramAdManager"

    // =========================================================================
    // 1. Master Configuration
    // =========================================================================

    // Keep ads disabled initially (set to true after 3-5 months when you're ready)
    @JvmStatic
    @Volatile
    var isEnabled: Boolean = false

    // Official Google AdMob Interstitial Test Ad Unit ID
    @JvmStatic
    @Volatile
    var interstitialAdUnitId: String = "ca-app-pub-3940256099942544/1033173712"

    // Display interval: exactly 15 minutes between interstitial ads
    const val DISPLAY_INTERVAL_MINUTES: Long = 15L
    const val DISPLAY_INTERVAL_MS: Long = DISPLAY_INTERVAL_MINUTES * 60 * 1000L

    // Minimum user actions (chat visits / navigations) before showing an ad
    const val MINIMUM_ACTIONS_BEFORE_AD: Int = 10

    // =========================================================================
    // 2. State Tracking
    // =========================================================================

    private var lastAdShownTimestamp: Long = 0L
    private var actionCounter: Int = 0
    private var isAdLoading: Boolean = false

    /**
     * Increments the user activity count (call on opening a chat or story).
     */
    @JvmStatic
    @Synchronized
    fun recordUserAction() {
        actionCounter++
    }

    /**
     * Checks if all criteria for showing an interstitial ad are fulfilled:
     * 1. Master switch is ON (isEnabled == true)
     * 2. At least 15 minutes have passed since the last ad
     * 3. User performed minimum required actions
     */
    @JvmStatic
    @Synchronized
    fun canShowInterstitial(): Boolean {
        if (!isEnabled) {
            return false
        }

        if (actionCounter < MINIMUM_ACTIONS_BEFORE_AD) {
            return false
        }

        val currentTime = SystemClock.elapsedRealtime()
        val timeSinceLastAd = currentTime - lastAdShownTimestamp

        return timeSinceLastAd >= DISPLAY_INTERVAL_MS
    }

    /**
     * Marks an interstitial ad as displayed and resets cooldown timer.
     */
    @JvmStatic
    @Synchronized
    fun onAdDisplayed() {
        lastAdShownTimestamp = SystemClock.elapsedRealtime()
        actionCounter = 0
        Log.i(TAG, "Interstitial displayed. Cooldown of $DISPLAY_INTERVAL_MINUTES minutes started.")
    }

    /**
     * Attempts to show interstitial if ready and cooldown has passed.
     * Returns true if ad display criteria was met.
     */
    @JvmStatic
    fun showInterstitialIfReady(activity: Activity): Boolean {
        if (canShowInterstitial()) {
            onAdDisplayed()
            // When AdMob SDK is initialized in the future, trigger the show() call here
            Log.d(TAG, "Ad condition satisfied. Showing interstitial.")
            return true
        }
        return false
    }

    /**
     * Resets the 15-minute timer manually if needed.
     */
    @JvmStatic
    @Synchronized
    fun resetTimer() {
        lastAdShownTimestamp = SystemClock.elapsedRealtime()
        actionCounter = 0
    }
}
