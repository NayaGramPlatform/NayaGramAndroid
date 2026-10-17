package org.nayagram.platform.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import org.nayagram.platform.monetization.INGMonetizationProvider
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NayaGramAdsConfig
import org.telegram.messenger.UserConfig

/**
 * Central gate for all NayaGram ad placements.
 *
 * Ads are opt-in at release level, are never shown over chats/calls, and
 * interstitials are capped to one per 20 minutes after meaningful usage.
 */
object AdManager {

    private const val TAG = "NayaGramMonetization"
    private const val PREFS_NAME = "nayagram_ads"
    private const val KEY_LAST_INTERSTITIAL_WALL_TIME = "last_interstitial_wall_time"

    @JvmStatic
    var isEnabled: Boolean
        get() = NayaGramAdsConfig.ENABLE_ADS
        set(value) {
            NayaGramAdsConfig.ENABLE_ADS = value
        }

    @JvmStatic
    @Volatile
    var activeProvider: INGMonetizationProvider? = null

    const val DISPLAY_INTERVAL_MINUTES: Long = 20L
    const val DISPLAY_INTERVAL_MS: Long = NayaGramAdsConfig.INTERSTITIAL_COOLDOWN_MS
    const val MINIMUM_ACTIONS_BEFORE_AD: Int = NayaGramAdsConfig.ACTIONS_BEFORE_INTERSTITIAL

    private val appSessionStartedWallTime: Long = System.currentTimeMillis()
    private var lastAdShownWallTime: Long = -1L
    private var actionCounter: Int = 0

    private fun isPremiumAccount(): Boolean {
        val account = UserConfig.selectedAccount
        return account in 0 until UserConfig.MAX_ACCOUNT_COUNT &&
            UserConfig.getInstance(account).isPremium()
    }

    @Synchronized
    private fun lastShownWallTime(): Long {
        if (lastAdShownWallTime < 0L) {
            lastAdShownWallTime = try {
                ApplicationLoader.applicationContext
                    ?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    ?.getLong(KEY_LAST_INTERSTITIAL_WALL_TIME, 0L) ?: 0L
            } catch (error: Exception) {
                Log.w(TAG, "Could not read persisted ad cooldown; failing closed.", error)
                System.currentTimeMillis()
            }
        }
        return lastAdShownWallTime
    }

    @JvmStatic
    @Synchronized
    fun recordUserAction() {
        if (isEnabled && !isPremiumAccount()) {
            actionCounter = (actionCounter + 1).coerceAtMost(MINIMUM_ACTIONS_BEFORE_AD)
        }
    }

    @JvmStatic
    @Synchronized
    fun canShowInterstitial(): Boolean {
        if (!isEnabled || isPremiumAccount()) return false

        val provider = activeProvider ?: return false
        if (!provider.isMonetizationEnabled) return false
        if (actionCounter < MINIMUM_ACTIONS_BEFORE_AD) return false

        val nowWall = System.currentTimeMillis()
        val lastShown = lastShownWallTime()
        // Do not show an interstitial immediately after a fresh install or app start.
        if (lastShown == 0L && nowWall - appSessionStartedWallTime < DISPLAY_INTERVAL_MS) return false
        // A clock change backwards must not cause ads to appear more frequently.
        if (nowWall < lastShown) return false
        return nowWall - lastShown >= DISPLAY_INTERVAL_MS
    }

    @JvmStatic
    @Synchronized
    fun onAdDisplayed() {
        val nowWall = System.currentTimeMillis()
        lastAdShownWallTime = nowWall
        actionCounter = 0
        try {
            ApplicationLoader.applicationContext
                ?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                ?.edit()
                ?.putLong(KEY_LAST_INTERSTITIAL_WALL_TIME, nowWall)
                ?.apply()
        } catch (error: Exception) {
            Log.w(TAG, "Could not persist ad cooldown.", error)
        }
        Log.i(TAG, "Interstitial impression recorded; 20-minute cooldown started.")
    }

    @JvmStatic
    fun showInterstitialIfReady(activity: Activity): Boolean {
        if (activity.isFinishing || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed)) {
            return false
        }
        if (!canShowInterstitial()) return false

        val provider = activeProvider ?: return false
        if (!provider.isMonetizationEnabled) return false

        return try {
            provider.showInterstitialIfAvailable(activity) { onAdDisplayed() }
        } catch (error: Exception) {
            Log.e(TAG, "Interstitial display failed; user flow will continue without an ad.", error)
            false
        }
    }

    @JvmStatic
    fun registerProvider(provider: INGMonetizationProvider) {
        activeProvider = provider
        Log.i(TAG, "Monetization provider registered: " + provider.javaClass.simpleName)
    }

    @JvmStatic
    @Synchronized
    fun resetTimer() {
        lastAdShownWallTime = System.currentTimeMillis()
        actionCounter = 0
    }
}
