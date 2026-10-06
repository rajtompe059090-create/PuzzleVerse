package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.core.Constants
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManager(private val context: Context) {

    private val tag = "AdMobManager"
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    private var lastInterstitialTimeMs: Long = 0

    init {
        try {
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

            MobileAds.initialize(context) { status ->
                Log.d(tag, "AdMob MobileAds initialized with status: $status")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize MobileAds", e)
        }
    }

    fun loadInterstitialAd() {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(tag, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(tag, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun loadRewardedAd() {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(tag, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(tag, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Show interstitial ad at meaningful transition points, respecting the frequency cooldown.
     * If the ad is not loaded or cooldown hasn't passed, simply continues the normal app flow.
     */
    fun showInterstitial(
        activity: Activity?,
        onDismiss: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        val cooldownPassed = (now - lastInterstitialTimeMs) >= Constants.INTERSTITIAL_COOLDOWN_MS

        if (activity != null && interstitialAd != null && cooldownPassed) {
            val ad = interstitialAd!!
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialTimeMs = System.currentTimeMillis()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    Log.w(tag, "Interstitial failed to show: ${adError.message}")
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            // Frequency cap or ad not loaded: do not block game switch
            if (interstitialAd == null && !isInterstitialLoading) {
                loadInterstitialAd()
            }
            onDismiss()
        }
    }

    fun showInterstitialOnGameSwitch(
        activity: Activity?,
        onDismiss: () -> Unit
    ) {
        showInterstitial(activity, onDismiss)
    }

    /**
     * Show rewarded ad for level completion reward claim.
     * The reward is STRICTLY granted only if Google's onUserEarnedReward callback was invoked.
     * Never grants reward on failure, cancellation, dismissal, or timeout.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        if (activity == null || rewardedAd == null) {
            Log.d(tag, "Rewarded ad not ready; using graceful fallback")
            loadRewardedAd()
            onAdUnavailable()
            return
        }

        var rewardGranted = false
        val ad = rewardedAd!!

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                if (rewardGranted) {
                    onRewardEarned()
                }
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                onAdUnavailable()
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            Log.d(tag, "User earned rewarded ad reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
}
