package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * NayaGram Official Ads Configuration & Controller
 * ------------------------------------------------
 * ইউজার-ফ্রেন্ডলি বিজ্ঞাপন কন্ট্রোল সিস্টেম।
 * প্রথম ৩-৫ মাস সম্পূর্ণ বিজ্ঞাপনমুক্ত থাকবে।
 * পরবর্তীতে বিজ্ঞাপন চালু করলেও ব্যবহারকারীদের যাতে কোনো বিরক্তি না হয়,
 * সেজন্য ১০ থেকে ১৫ মিনিট (ডিফল্ট ১২ মিনিট) ব্যবধানে বিজ্ঞাপন দেখানো হবে।
 */
public class NayaGramAdsConfig {

    // =========================================================================
    // ১. মাস্টার সুইচ (Master Switch)
    // -------------------------------------------------------------------------
    // প্রথম ৩ থেকে ৫ মাস সম্পূর্ণ বিজ্ঞাপনমুক্ত থাকবে (ডিফল্ট: false)।
    // ভবিষ্যতে যখন আপনি বিজ্ঞাপন রান করতে চাইবেন, তখন শুধু 'false' এর জায়গায় 'true' লিখে দিন:
    // =========================================================================
    public static boolean ENABLE_ADS = false;

    // =========================================================================
    // ২. গুগল অ্যাডমব আইডি (Google AdMob IDs)
    // -------------------------------------------------------------------------
    // ডিফল্টভাবে গুগলের অফিশিয়াল টেস্ট আইডি দেওয়া আছে (যাতে কোনো ভায়োলেশন না হয়)।
    // ভবিষ্যতে ৩-৫ মাস পর যখন অ্যাডমব অ্যাকাউন্ট খুলবেন, তখন আপনার আসল আইডিগুলো এখানে বসাবেন:
    // =========================================================================
    public static String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";       // ব্যানার অ্যাড
    public static String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // ফুল স্ক্রিন অ্যাড
    public static String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";       // নেটিভ কার্ড অ্যাড

    // =========================================================================
    // ৩. সুপার ইউজার-ফ্রেন্ডলি টাইমিং (১০ থেকে ১৫ মিনিট পর পর অ্যাড)
    // =========================================================================
    
    // ফুল স্ক্রিন (Interstitial) অ্যাড প্রতি ১২ মিনিটে (৭২০ সেকেন্ড) সর্বোচ্চ ১ বার আসতে পারে
    // ফলে ব্যবহারকারীরা মন খুলে চ্যাট করতে পারবে এবং কোনো প্রকার বিরক্তি অনুভব করবে না
    public static final long INTERSTITIAL_COOLDOWN_MS = 12 * 60 * 1000L; // ১২ মিনিট (৭২০,০০০ মিলি-সেকেন্ড)

    // ব্যবহারকারী কমপক্ষে ১০টি অ্যাকশন (চ্যাট পরিবর্তন বা স্টোরি দেখা) সম্পন্ন করার পর সুযোগ আসবে
    public static final int ACTIONS_BEFORE_INTERSTITIAL = 10; 

    // চ্যাটের ভেতর লেখার জায়গা সম্পূর্ণ ক্লিন থাকবে (চ্যাটে কোনো ব্যানার আসবে না)
    public static final boolean SHOW_BANNER_INSIDE_CHATS = false;

    // শুধুমাত্র হোম স্ক্রিনের চ্যাট তালিকার উপরে বা সাইড ড্রয়ার মেনুর নিচে ছোট ব্যানার দেখানো যাবে
    public static final boolean SHOW_BANNER_IN_DIALOGS = true;

    // -------------------------------------------------------------------------
    // স্টেট ট্র্যাকিং ভেরিয়েবল
    // -------------------------------------------------------------------------
    private static long lastInterstitialShownTime = 0;
    private static int actionCounter = 0;

    /**
     * ফুল স্ক্রিন অ্যাড দেখানোর উপযুক্ত সময় হয়েছে কি না তা স্বয়ংক্রিয়ভাবে যাচাই করে
     */
    public static boolean shouldShowInterstitial() {
        if (!ENABLE_ADS) {
            return false;
        }

        actionCounter++;
        if (actionCounter < ACTIONS_BEFORE_INTERSTITIAL) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastInterstitialShownTime >= INTERSTITIAL_COOLDOWN_MS) {
            lastInterstitialShownTime = now;
            actionCounter = 0;
            return true;
        }

        return false;
    }

    /**
     * হোম স্ক্রিন বা ড্রয়ার ব্যানারের জন্য চেক
     */
    public static boolean shouldShowBanner() {
        return ENABLE_ADS && SHOW_BANNER_IN_DIALOGS;
    }
}
