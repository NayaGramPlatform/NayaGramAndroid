package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * NayaGram Official Ads Configuration & Controller
 * ------------------------------------------------
 * এই ক্লাসের মাধ্যমে খুব সহজে ভবিষ্যতে একটিমাত্র শব্দ (true/false) পরিবর্তন করে 
 * Google AdMob বা যেকোনো অ্যাড নেটওয়ার্ক চালু অথবা বন্ধ করা যাবে।
 */
public class NayaGramAdsConfig {

    // =========================================================================
    // ১. মাস্টার সুইচ (Master Switch)
    // -------------------------------------------------------------------------
    // ভবিষ্যতে বিজ্ঞাপন চালু করতে নিচের 'false' শব্দটিকে শুধু 'true' লিখে দিন:
    // =========================================================================
    public static boolean ENABLE_ADS = false;

    // =========================================================================
    // ২. গুগল অ্যাডমব আইডি (Google AdMob IDs)
    // -------------------------------------------------------------------------
    // ডিফল্টভাবে গুগলের অফিশিয়াল টেস্ট আইডি দেওয়া আছে (যাতে অ্যাকাউন্ট ব্যান না হয়)।
    // ভবিষ্যতে আপনি নিজের আসল AdMob App ID এবং Unit ID এখানে পেস্ট করবেন:
    // =========================================================================
    public static String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";       // ব্যানার অ্যাড
    public static String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // ফুল স্ক্রিন অ্যাড
    public static String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";       // নেটিভ কার্ড অ্যাড

    // =========================================================================
    // ৩. ইউজার ফ্রেন্ডলি টাইমিং ও ফ্রিকোয়েন্সি (ইউজারদের বিরক্তি কমানোর সেটিংস)
    // =========================================================================
    
    // ফুল স্ক্রিন (Interstitial) অ্যাড প্রতি ৩ মিনিটের (১৮০ সেকেন্ড) মধ্যে ১ বারের বেশি আসবে না
    public static final long INTERSTITIAL_COOLDOWN_MS = 180 * 1000L; 

    // ব্যবহারকারী কমপক্ষে ৫টি চ্যাট বা স্টোরি দেখার পর প্রথমবার অ্যাডের সুযোগ পাবে
    public static final int ACTIONS_BEFORE_INTERSTITIAL = 5; 

    // চ্যাটিং স্ক্রিনের ভেতরে কোনো ব্যানার অ্যাড থাকবে না (চ্যাট থাকবে ১০০% ক্লিন ও ফাস্ট)
    public static final boolean SHOW_BANNER_INSIDE_CHATS = false;

    // হোম স্ক্রিনের চ্যাট তালিকার উপরে বা সাইড ড্রয়ারে অ্যাড সক্রিয় থাকবে
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
