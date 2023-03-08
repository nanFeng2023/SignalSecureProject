package com.ssv.signalsecurevpn.ad

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.ssv.signalsecurevpn.App
import com.ssv.signalsecurevpn.bean.AdBean
import com.ssv.signalsecurevpn.bean.AdDataResult
import com.ssv.signalsecurevpn.bean.AdWrap
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.AdUtil
import com.ssv.signalsecurevpn.util.NetworkUtil
import timber.log.Timber
import java.util.Date

/*AdMob*/
object AdMob : AbstractAd() {
    const val AD_OPEN = "open"
    const val AD_INTER_CLICK = "inter-click"
    const val AD_INTER_IB = "inter-ib"
    const val AD_NATIVE_HOME = "native_home"
    const val AD_NATIVE_RESULT = "native_result"
    const val SIGVN_AD = "sigvn_ad"
    var reReqLoadAd = true
    private const val SHOW_AD_NUM_MAX_DAY = 50
    private const val CLICK_AD_NUM_MAX_DAY = 15
    var isReqInterrupt = false

    private var adWrapHashMap: HashMap<String, AdWrap> = HashMap()
    private var adBeanList: ArrayList<AdBean>? = null

    override fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadAd()---接口数据adDataResult:${NetworkUtil.adDataResult}")
        val wrap = adWrapHashMap[type]
        if (wrap?.isAdLoading == true || wrap?.isAdAvailable() == true) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadAd()---广告正在请求或有广告可以使用，不用加载")
            return
        }

        val adDataResult = NetworkUtil.adDataResult ?: return//如果对象为空就返回
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadAd()---adDataResult:$adDataResult")
        val ssvShowUpperLimit = adDataResult.ssv_show_upper_limit
        val ssvClickUpperLimit = adDataResult.ssv_click_upper_limit

        val adWrap = AdWrap(type)
        if (adLoadStateCallBack != null) {
            adWrap.adLoadStateCallBack = adLoadStateCallBack
        }
        adWrapHashMap[adWrap.adType] = adWrap

        //广告展示或点击是否达到每日数量上限
        if (isReachDailyMaxLimit(ssvShowUpperLimit, ssvClickUpperLimit)) {
            adWrap.adLoadStateCallBack?.onAdLoaded()
            isReqInterrupt = true
            return
        }
        isReqInterrupt = false
        adBeanList = getAdBeanList(type, adDataResult)
        loadAd(type)
    }

    private fun getAdBeanList(adType: String, adDataResult: AdDataResult): ArrayList<AdBean>? {
        var list: ArrayList<AdBean>? = null
        if (AD_OPEN == adType) {
            list = adDataResult.ssv_ad_open_on
        } else if (AD_INTER_CLICK == adType) {
            list = adDataResult.ssv_ad_inter_click
        } else if (AD_INTER_IB == adType) {
            list = adDataResult.ssv_ad_inter_ib
        } else if (AD_NATIVE_HOME == adType) {
            list = adDataResult.ssv_ad_native_home
        } else if (AD_NATIVE_RESULT == adType) {
            list = adDataResult.ssv_ad_native_result
        }
        return list
    }

    private fun loadAd(adType: String) {
        var adBean: AdBean? = null
        if (adBeanList?.iterator()?.hasNext() == true) {
            adBean = adBeanList!!.iterator().next()
        }
        if (adBean == null)
            return

        if (AD_OPEN == adType) {
            loadOpenAd(adBean, adType)
        } else if (AD_INTER_CLICK == adType || AD_INTER_IB == adType) {
            loadInterAd(adBean, adType)
        } else if (AD_NATIVE_HOME == adType || AD_NATIVE_RESULT == adType) {
            loadNativeAd()
        }
    }

    /*是否达到每日最大限制*/
    private fun isReachDailyMaxLimit(showNumLimit: Int, clickNumLimit: Int = 0): Boolean {
        return showNumLimit >= SHOW_AD_NUM_MAX_DAY || clickNumLimit >= CLICK_AD_NUM_MAX_DAY
    }

    private fun loadNativeAd() {

    }

    private fun loadInterAd(adBean: AdBean, adType: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadInterAd()")
        val adWrap = adWrapHashMap[adType]
        if (adWrap?.isAdLoading!! || adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadInterAd()---广告正在请求或有广告可以使用，不用加载")
            return
        }

        adBean.ssv_id?.let {
            val request = AdRequest.Builder().build()
            adWrap.isAdLoading = true
            InterstitialAd.load(App.appContext, it, request, object :
                InterstitialAdLoadCallback() {
                override fun onAdLoaded(interAd: InterstitialAd) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----loadInterAd()---onAdLoaded()")
                    onLoadSuccessSetAdWrapState(interAd, adWrap)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----loadInterAd()---onAdFailedToLoad()")
                    onLoadFailSetAdWrapState(adWrap)
                }
            })
        }
    }

    private fun loadOpenAd(adBean: AdBean, adType: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadOpenAd()")
        val adWrap = adWrapHashMap[adType]
        if (adWrap?.isAdLoading!! || adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadOpenAd()---广告正在请求或有广告可以使用，不用加载")
            return
        }

        val request = AdRequest.Builder().build()
        adBean.ssv_id?.let {//广告ID不为空才开始加载
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadOpenAd()---开始请求广告,优先级：${adBean.ssv_priority},广告ID：${adBean.ssv_id}")
            adWrap.isAdLoading = true
            AppOpenAd.load(
                App.appContext,
                it,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("AdMob----onAdLoaded()")
                        onLoadSuccessSetAdWrapState(ad, adWrap)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("AdMob----onAdFailedToLoad()--loadAdError:$loadAdError")
                        onLoadFailSetAdWrapState(adWrap)

//                        loadAd()
                        if (reReqLoadAd) {//请求失败再次发起请求广告
                            reReqLoadAd = false
                            Timber.tag(ConfigurationUtil.LOG_TAG)
                                .d("AdMob----onAdFailedToLoad()--再次请求广告")


                        } else {
                            Timber.tag(ConfigurationUtil.LOG_TAG)
                                .d("AdMob----onAdFailedToLoad()--再次请求广告失败")
                            adWrap.adLoadStateCallBack?.onAdLoadFail()
                        }
                    }
                })
        }
    }

    private fun onLoadSuccessSetAdWrapState(ad: Any, adWrap: AdWrap) {
        adWrap.isAdLoading = false
        adWrap.ad = ad
        adWrap.expirationTime = Date().time
        adWrap.adLoadStateCallBack?.onAdLoaded()
    }

    private fun onLoadFailSetAdWrapState(adWrap: AdWrap) {
        adWrap.isAdLoading = false
        adWrap.ad = null
    }

    override fun showAd(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?
    ) {
        if (!AdUtil.activityIsResume(activity)) {//页面是否可见
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----showOpenAd()---当前页面不可见，页面状态：${activity.lifecycle.currentState}")
            adShowStateCallBack?.onAdShowFail()
            return
        }
        var adWrap = adWrapHashMap[adType]
        val ad = adWrap?.ad
        if (adWrap == null || ad == null) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----showOpenAd()---no such adWrap or ad")
            adShowStateCallBack?.onAdShowFail()
            return
        }

        if (!adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----The app open ad is not ready yet.")
            adShowStateCallBack?.onAdShowFail()
            return
        }

        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----Will show ad")

        val callback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {//该回调是点击广告关闭时触发
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----showOpenAd()---onAdDismissedFullScreenContent()")
                adShowStateCallBack?.onAdDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----showOpenAd()---onAdShowedFullScreenContent()")
                adWrapHashMap.remove(adType)
                adWrap = null
                adShowStateCallBack?.onAdShowed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----showOpenAd()---onAdFailedToShowFullScreenContent()")
                adWrapHashMap.remove(adType)
                adWrap = null
                adShowStateCallBack?.onAdShowFail()
            }

            override fun onAdClicked() {

            }
        }

        when (ad) {
            is AppOpenAd -> {
                ad.fullScreenContentCallback = callback
                ad.show(activity)
            }
            is InterstitialAd -> {
                ad.fullScreenContentCallback = callback
                ad.show(activity)
            }

            is NativeAd -> {

            }
            else -> {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----showOpenAd()---no such ad:$ad")
            }
        }
    }

    override fun isAdAvailable(adType: String): Boolean {
        return adWrapHashMap[adType]?.isAdAvailable() ?: false
    }
}