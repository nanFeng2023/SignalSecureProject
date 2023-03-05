package com.ssv.signalsecurevpn

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.ssv.signalsecurevpn.util.AdConfigurationUtil
import com.ssv.signalsecurevpn.util.AdUtil
import timber.log.Timber
import java.util.Date

/*广告Manager*/
object AdManager {
    var appOpenAd: AppOpenAd? = null

    private var loadTime: Long = 0
    var reReqLoadAd = true
    var onAdStateListener: OnAdStateListener? = null

    fun loadOpenAd() {
        Timber.tag(AdConfigurationUtil.LOG_TAG)
            .d("AdManager----loadOpenAd()---appOpenAd:$appOpenAd")
        if (isOpenAdAvailable()) {
            Timber.tag(AdConfigurationUtil.LOG_TAG)
                .d("AdManager----loadOpenAd()---有广告可以使用，不用加载")
            return
        }
        Timber.tag(AdConfigurationUtil.LOG_TAG).d("AdManager----loadOpenAd()---开始请求广告")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(App.appContext, AdConfigurationUtil.AD_UNIT_ID, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    Timber.tag(AdConfigurationUtil.LOG_TAG)
                        .d("AdManager----onAdLoaded()")
                    onAdStateListener?.onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    Timber.tag(AdConfigurationUtil.LOG_TAG)
                        .d("AdManager----onAdFailedToLoad()--loadAdError:$loadAdError")
                    if (reReqLoadAd) {//请求失败再次发起请求广告
                        reReqLoadAd = false
                        Timber.tag(AdConfigurationUtil.LOG_TAG)
                            .d("AdManager----onAdFailedToLoad()--再次请求广告")
                        loadOpenAd()
                    } else {
                        Timber.tag(AdConfigurationUtil.LOG_TAG)
                            .d("AdManager----onAdFailedToLoad()--再次请求广告失败")
                        onAdStateListener?.onAdLoadFail()
                    }
                }
            })
    }

    fun showOpenAd(activity: AppCompatActivity) {
        if (!AdUtil.activityIsResume(activity)) {//页面是否可见
            Timber.tag(AdConfigurationUtil.LOG_TAG)
                .d("AdManager----showOpenAd()---当前页面不可见，页面状态：${activity.lifecycle.currentState}")
            return
        }

        if (!isOpenAdAvailable()) {
            Timber.tag(AdConfigurationUtil.LOG_TAG)
                .d("AdManager----The app open ad is not ready yet.")
            return
        }
        Timber.tag(AdConfigurationUtil.LOG_TAG)
            .d("AdManager----Will show ad---appOpenAd:$appOpenAd")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {//该回调是点击广告关闭时触发
                Timber.tag(AdConfigurationUtil.LOG_TAG)
                    .d("AdManager----showOpenAd()---onAdDismissedFullScreenContent()")
                appOpenAd = null
                onAdStateListener?.onShowAdComplete()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(AdConfigurationUtil.LOG_TAG)
                    .d("AdManager----showOpenAd()---onAdShowedFullScreenContent()")
                appOpenAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Timber.tag(AdConfigurationUtil.LOG_TAG)
                    .d("AdManager----showOpenAd()---onAdFailedToShowFullScreenContent()")
                appOpenAd = null
                onAdStateListener?.onShowAdComplete()
            }
        }
        appOpenAd?.show(activity)
    }

    private fun isOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(AdConfigurationUtil.AD_EXPIRATION_TIME)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {//判断广告是否过期
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    interface OnAdStateListener {
        fun onAdLoaded()
        fun onAdLoadFail()
        fun onShowAdComplete()
    }
}