package com.yakup.bluescreen

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Merkezi AdMob yöneticisi.
 *
 * Kullanım:
 *   AdManager.init(applicationContext)
 *   AdManager.loadBanner(activity, container)
 *   AdManager.loadInterstitial(context)
 *   AdManager.showInterstitial(activity)
 */
object AdManager {

    private const val TAG = "AdManager"

    // ─── Test ID'leri – canlıya geçerken bunları değiştir ───────────────────
    // Gerçek ID'leri AdMob konsolundan (https://apps.admob.com) alırsın.
    // Format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
    private const val BANNER_ID       = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var interstitialLoadAttempts = 0
    private const val MAX_LOAD_ATTEMPTS = 3

    // ─── Başlatma ────────────────────────────────────────────────────────────

    /**
     * Uygulama başlangıcında bir kere çağır (Application.onCreate veya MainActivity.onCreate).
     */
    fun init(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { initStatus ->
            isInitialized = true
            Log.d(TAG, "AdMob başlatıldı: $initStatus")
        }
    }

    // ─── Banner ──────────────────────────────────────────────────────────────

    /**
     * Verilen [container] ViewGroup'a banner ekler.
     * container genellikle activity_main.xml içindeki bannerContainer LinearLayout'u.
     */
    fun loadBanner(activity: Activity, container: ViewGroup) {
        val adView = AdView(activity).apply {
            adUnitId = BANNER_ID
            setAdSize(AdSize.BANNER)
        }

        container.removeAllViews()
        container.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner yüklendi")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner yüklenemedi: ${error.message}")
                container.removeAllViews() // Başarısız olursa container'ı gizle
            }
        }
        adView.loadAd(adRequest)
    }

    // ─── Interstitial ────────────────────────────────────────────────────────

    /**
     * Interstitial reklamı önceden yükler.
     * Göstermeden önce loadInterstitial() çağrılmış olmalı.
     */
    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return // Zaten yüklü
        if (interstitialLoadAttempts >= MAX_LOAD_ATTEMPTS) return

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoadAttempts = 0
                    Log.d(TAG, "Interstitial yüklendi")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial(context) // Kapatıldıktan sonra yeniden yükle
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            Log.w(TAG, "Interstitial gösterilemedi: ${error.message}")
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    interstitialLoadAttempts++
                    Log.w(TAG, "Interstitial yüklenemedi ($interstitialLoadAttempts/$MAX_LOAD_ATTEMPTS): ${error.message}")
                }
            }
        )
    }

    /**
     * Yüklü interstitial'ı gösterir. Yüklü değilse yeniden yüklemeye çalışır.
     * @return true → reklam gösterildi, false → henüz hazır değil
     */
    fun showInterstitial(activity: Activity): Boolean {
        val ad = interstitialAd
        return if (ad != null) {
            ad.show(activity)
            true
        } else {
            loadInterstitial(activity)
            false
        }
    }

    /** Interstitial yüklü ve gösterime hazır mı? */
    val isInterstitialReady: Boolean
        get() = interstitialAd != null
}
