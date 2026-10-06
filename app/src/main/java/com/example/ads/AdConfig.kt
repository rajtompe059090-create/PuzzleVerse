package com.example.ads

/**
 * AdMob configuration with official production ad units.
 */
object AdConfig {
    // Production AdMob Application ID
    const val ADMOB_APP_ID = "ca-app-pub-6146868530948467~2073205894"

    // Production Ad Unit IDs
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6146868530948467/9742803195"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6146868530948467/5525365749"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6146868530948467/3177394847"

    // Standard Google Sample Test Ad Unit IDs for development testing
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Set to false to use live production ad unit IDs across all builds
    const val IS_TEST_ADS = false

    // Active Ad Unit IDs used throughout the app
    val BANNER_AD_UNIT_ID: String
        get() = if (IS_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (IS_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (IS_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
}
