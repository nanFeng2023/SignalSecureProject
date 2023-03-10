package com.ssv.signalsecurevpn.ad

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.ssv.signalsecurevpn.App
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.bean.AdBean
import com.ssv.signalsecurevpn.bean.AdDataResult
import com.ssv.signalsecurevpn.bean.AdWrap
import com.ssv.signalsecurevpn.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

/*AdMob*/
object AdMob : AbstractAd() {
    const val AD_OPEN = "open"
    const val AD_INTER = "inter"
    const val AD_NATIVE = "native"
    const val AD_INTER_CLICK = "inter-click"
    const val AD_INTER_IB = "inter-ib"
    const val AD_NATIVE_HOME = "native_home"
    const val AD_NATIVE_RESULT = "native_result"
    const val SIGVN_SERVICE = "sigvn_service"
    const val SIGVN_SMART_SERVICE = "sigvn_smart_service"
    const val SIGVN_AD = "sigvn_ad"
    var isReqInterrupt = false

    private var adWrapHashMap: HashMap<String, AdWrap> = HashMap()
    var isRefreshNativeAd = false
    private var isOverDayLimit = false
    private var ssvShowUpperLimit = 0
    private var ssvClickUpperLimit = 0
    override fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?) {
        val wrap = adWrapHashMap[type]
        if (wrap?.isAdLoading == true || wrap?.isAdAvailable() == true) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadAd()---???????????????$type---?????????????????????????????????????????????????????????")
            return
        }

        val adDataResult = NetworkUtil.adDataResult ?: return//???????????????????????????
        ssvShowUpperLimit = adDataResult.ssv_show_upper_limit
        ssvClickUpperLimit = adDataResult.ssv_click_upper_limit

        val adWrap = wrap ?: AdWrap(type)
        if (adLoadStateCallBack != null) {
            adWrap.adLoadStateCallBack = adLoadStateCallBack
        }
        adWrapHashMap[adWrap.adType] = adWrap

        if (isCanLoadAd(adWrap, type))
            return

        isReqInterrupt = false
        adWrap.adBeanList = getAdBeanList(type, adDataResult)
        loadAd(type, adWrap)
    }

    private fun isCanLoadAd(adWrap: AdWrap, adType: String): Boolean {
        var long = SharePreferenceUtil.getLong(ProjectUtil.LAST_AD_SHOW_TIME)
        if (long == 0L) {
            long = System.currentTimeMillis()
            SharePreferenceUtil.putLong(ProjectUtil.LAST_AD_SHOW_TIME, long)
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d(
                    "AdMob----???????????????$adType---isCanLoadAd()---?????????????????????????????????---?????????${
                        ProjectUtil.longToYMDHMS(
                            long
                        )
                    }"
                )
        }
        val longToYMDHMS = ProjectUtil.longToYMDHMS(long)
        val today = ProjectUtil.isToday(longToYMDHMS)
        var adShowNum = SharePreferenceUtil.getInt(ProjectUtil.AD_SHOW_NUM)
        var adClickNum = SharePreferenceUtil.getInt(ProjectUtil.AD_CLICK_NUM)
        if (!today) {//???????????????????????????????????????????????????????????????????????????????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----???????????????$adType---isCanLoadAd()---?????????????????????????????????1")
            adShowNum = 0
            adClickNum = 0
            SharePreferenceUtil.putLong(ProjectUtil.LAST_AD_SHOW_TIME, System.currentTimeMillis())
            SharePreferenceUtil.putInt(ProjectUtil.AD_SHOW_NUM, adShowNum)
            SharePreferenceUtil.putInt(ProjectUtil.AD_CLICK_NUM, adClickNum)
            isOverDayLimit = false
            return false
        } else {
            //???????????????????????????????????????????????????
            if (isReachDailyMaxLimit(adShowNum, adClickNum)) {
                adWrap.adLoadStateCallBack?.onAdLoaded()
                isOverDayLimit = true
                isReqInterrupt = true
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----isCanLoadAd()---???????????????$adType---adShowNum:$adShowNum---adClickNum:$adClickNum---???????????????????????????????????????????????????????????????")
                return true
            }
            return false
        }
    }

    private fun getAdBeanList(adType: String, adDataResult: AdDataResult): ArrayList<AdBean>? {
        val list: ArrayList<AdBean> = ArrayList()
        if (AD_OPEN == adType) {
            adDataResult.ssv_ad_open_on?.let { list.addAll(it) }
        } else if (AD_INTER_CLICK == adType) {
            adDataResult.ssv_ad_inter_click?.let { list.addAll(it) }
        } else if (AD_INTER_IB == adType) {
            adDataResult.ssv_ad_inter_ib?.let { list.addAll(it) }
        } else if (AD_NATIVE_HOME == adType) {
            adDataResult.ssv_ad_native_home?.let { list.addAll(it) }
        } else if (AD_NATIVE_RESULT == adType) {
            adDataResult.ssv_ad_native_result?.let { list.addAll(it) }
        }
        list.sortByDescending {//???????????????
            it.ssv_priority
        }
        return list
    }

    private fun loadAd(adType: String, adWrap: AdWrap) {
        val iterator = adWrap.adBeanList?.iterator()
        var adBean: AdBean? = null
        if (iterator?.hasNext() == true) {
            adBean = iterator.next()
            iterator.remove()//???????????????
        }
        if (adBean == null)
            return
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadAd()---???????????????$adType---??????????????????")
        if (AD_OPEN == adType) {
            if (adBean.ssv_type == AD_OPEN) {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----loadAd()---???????????????$adType---open????????????????????????")
                loadOpenAd(adBean, adType)
            } else if (adBean.ssv_type == AD_INTER) {//??????????????????????????????
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----loadAd()---???????????????$adType---inter????????????????????????")
                loadInterAd(adBean, adType)
            }
        } else if (AD_INTER_CLICK == adType || AD_INTER_IB == adType) {
            loadInterAd(adBean, adType)
        } else if (AD_NATIVE_HOME == adType || AD_NATIVE_RESULT == adType) {
            loadNativeAd(adBean, adType)
        }
    }

    /*??????????????????????????????*/
    private fun isReachDailyMaxLimit(adShowNum: Int, adClickNum: Int): Boolean {
        return adShowNum >= ssvShowUpperLimit || adClickNum >= ssvClickUpperLimit
    }

    private fun loadNativeAd(adBean: AdBean, adType: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadNativeAd()---???????????????$adType")
        val adWrap = adWrapHashMap[adType]
        if (adWrap?.isAdLoading!! || adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadNativeAd()---???????????????$adType---?????????????????????????????????????????????????????????")
            return
        }
        adBean.ssv_id?.let {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadNativeAd()---???????????????$adType---??????????????????,????????????${adBean.ssv_priority},??????ID???${adBean.ssv_id}")
            adWrap.isAdLoading = true
            val builder = AdLoader.Builder(App.appContext, adBean.ssv_id!!)
            val adLoader = builder.forNativeAd {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----loadNativeAd()---???????????????$adType---forNativeAd()---nativeAd:$it")
                onLoadSuccessSetAdWrapState(it, adWrap)
            }.withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    adClick(adType)
                }

                override fun onAdFailedToLoad(loadError: LoadAdError) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----loadNativeAd()---???????????????$adType---onAdFailedToLoad()---loadError:$loadError")
                    onLoadFailSetAdWrapState(adWrap)
                    adWrap.adLoadStateCallBack?.onAdLoadFail()
                    reloadAd(adType, adWrap)
                }
            }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun loadInterAd(adBean: AdBean, adType: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadInterAd()---???????????????$adType")
        val adWrap = adWrapHashMap[adType]
        if (adWrap?.isAdLoading!! || adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadInterAd()---???????????????$adType---?????????????????????????????????????????????????????????")
            return
        }

        adBean.ssv_id?.let {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadInterAd()---???????????????$adType---??????????????????,????????????${adBean.ssv_priority},??????ID???${adBean.ssv_id}")
            adWrap.isAdLoading = true
            val request = AdRequest.Builder().build()
            InterstitialAd.load(App.appContext, it, request, object :
                InterstitialAdLoadCallback() {
                override fun onAdLoaded(interAd: InterstitialAd) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----loadInterAd()---???????????????$adType---onAdLoaded()")
                    onLoadSuccessSetAdWrapState(interAd, adWrap)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----loadInterAd()---???????????????$adType---onAdFailedToLoad():$p0")
                    onLoadFailSetAdWrapState(adWrap)
                    reloadAd(adType, adWrap)
                }
            })
        }
    }

    private fun loadOpenAd(adBean: AdBean, adType: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----loadOpenAd()---???????????????$adType")
        val adWrap = adWrapHashMap[adType]
        if (adWrap?.isAdLoading!! || adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadOpenAd()---???????????????$adType---?????????????????????????????????????????????????????????")
            return
        }

        val request = AdRequest.Builder().build()
        adBean.ssv_id?.let {//??????ID????????????????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----loadOpenAd()---???????????????$adType---??????????????????,????????????${adBean.ssv_priority},??????ID???${adBean.ssv_id}")
            adWrap.isAdLoading = true
            AppOpenAd.load(
                App.appContext,
                it,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("AdMob----???????????????$adType---onAdLoaded()")
                        onLoadSuccessSetAdWrapState(ad, adWrap)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("AdMob----???????????????$adType---onAdFailedToLoad()--loadAdError:$loadAdError")
                        onLoadFailSetAdWrapState(adWrap)
                        reloadAd(adType, adWrap)
                    }
                })
        }
    }

    private fun isHaveAdNoReq(adWrap: AdWrap): Boolean {
        return adWrap.adBeanList?.iterator()?.hasNext() == true
    }

    private fun reloadAd(adType: String, adWrap: AdWrap) {
        if (isHaveAdNoReq(adWrap)) {//???????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----???????????????$adType---onAdFailedToLoad()--???????????????????????????")
            loadAd(adType, adWrap)
        } else {
            if (adType == AD_OPEN) {//???????????????????????????
                if (adWrap.isReload) {//????????????????????????????????????
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----???????????????$adType---onAdFailedToLoad()--??????????????????")
                    adWrap.isReload = false
                    loadAd(adType, adWrap.adLoadStateCallBack)
                } else {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("AdMob----???????????????$adType---onAdFailedToLoad()--????????????????????????")
                    adWrap.adLoadStateCallBack?.onAdLoadFail()
                }
            }
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
        adShowStateCallBack: AdShowStateCallBack?,
        layoutId: Int?,
        nativeAdParentGroup: ViewGroup?
    ) {
        //??????50s??????????????????????????????
        activity.lifecycleScope.launch {
            delay(50)
            showAdvertise(activity, adType, adShowStateCallBack, layoutId, nativeAdParentGroup)
        }
    }

    private fun showAdvertise(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?,
        layoutId: Int?,
        nativeAdParentGroup: ViewGroup?
    ) {
        if (!AdUtil.activityIsResume(activity)) {//??????????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----showAd()---???????????????$adType---???????????????????????????????????????${activity.lifecycle.currentState}")
            adShowStateCallBack?.onAdShowFail()
            return
        }
        var adWrap = adWrapHashMap[adType]
        val ad = adWrap?.ad
        if (adWrap == null || ad == null) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----showAd()---???????????????$adType---no such adWrap or ad")
            adShowStateCallBack?.onAdShowFail()
            return
        }

        if (!adWrap.isAdAvailable()) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("AdMob----???????????????$adType---The app open ad is not ready yet.")
            adShowStateCallBack?.onAdShowFail()
            return
        }

        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----???????????????$adType---showAd()---Will show ad")

        val callback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {//???????????????????????????????????????
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showOpenAd()---onAdDismissedFullScreenContent()")
                adShowStateCallBack?.onAdDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showOpenAd()---onAdShowedFullScreenContent()")
                adWrapHashMap.remove(adType)
                adWrap = null
                adShowStateCallBack?.onAdShowed()
                adShow(adType)
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showOpenAd()---onAdFailedToShowFullScreenContent()")
                adWrapHashMap.remove(adType)
                adWrap = null
                adShowStateCallBack?.onAdShowFail()
            }

            override fun onAdClicked() {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showOpenAd()---onAdClicked()")
                adShowStateCallBack?.onAdClicked()
                adClick(adType)
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
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showAd()---????????????????????????")
                createNativeAdView(
                    activity,
                    layoutId,
                    ad,
                    nativeAdParentGroup,
                    adWrap, adType,
                    adShowStateCallBack
                )
            }
            else -> {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---showOpenAd()---no such ad:$ad")
            }
        }

    }

    private fun adClick(adType: String) {
        var adClickNum = SharePreferenceUtil.getInt(ProjectUtil.AD_CLICK_NUM)
        adClickNum++
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----???????????????$adType---adClick()---adClickNum:$adClickNum")
        SharePreferenceUtil.putInt(ProjectUtil.AD_CLICK_NUM, adClickNum)
    }

    private fun adShow(adType: String) {
        var adShowNum = SharePreferenceUtil.getInt(ProjectUtil.AD_SHOW_NUM)
        adShowNum++
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("AdMob----???????????????$adType---adShow()---adShowNum:$adShowNum")
        SharePreferenceUtil.putInt(ProjectUtil.AD_SHOW_NUM, adShowNum)
    }

    private fun createNativeAdView(
        activity: AppCompatActivity,
        layoutId: Int?,
        nativeAd: NativeAd,
        nativeAdParentGroup: ViewGroup?,
        adWrap: AdWrap?,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?
    ) {
        val nativeAdView =
            layoutId?.let { activity.layoutInflater.inflate(it, null) } as NativeAdView
        nativeAdView.run {
            mediaView = findViewById(R.id.mv_ad)
            callToActionView = findViewById<TextView>(R.id.tv_ad_call_to_action)
            headlineView = findViewById<TextView>(R.id.tv_ad_headline)
            iconView = findViewById<ImageView>(R.id.iv_ad_icon)
            advertiserView = findViewById<TextView>(R.id.tv_ad_advertiser)
            nativeAd.mediaContent?.let {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("AdMob----???????????????$adType---createNativeAdView()---mediaContent:$it")
                mediaView?.mediaContent = it
            }
            (headlineView as TextView?)?.text = nativeAd.headline
            (advertiserView as TextView?)?.text = nativeAd.advertiser
            if (nativeAd.icon == null) {
                (iconView as ImageView?)?.visibility = View.INVISIBLE
            } else {
                (iconView as ImageView?)?.setImageDrawable(nativeAd.icon?.drawable)
                (iconView as ImageView?)?.visibility = View.VISIBLE
            }
            if (nativeAd.callToAction == null) {
                (callToActionView as TextView?)?.visibility = View.INVISIBLE
            } else {
                (callToActionView as TextView?)?.visibility = View.VISIBLE
                (callToActionView as TextView?)?.text = nativeAd.callToAction
            }

            nativeAdView.setNativeAd(nativeAd)
            nativeAdParentGroup?.removeAllViews()
            nativeAdParentGroup?.addView(nativeAdView)
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        activity.lifecycle.removeObserver(this)
                        nativeAdView.destroy()
                    }
                }
            })
            adWrap?.ad = null
            adWrapHashMap.remove(adType)
            adShow(adType)
            adShowStateCallBack?.onAdShowed()
        }
    }

    override fun isAdAvailable(adType: String): Boolean {
        return adWrapHashMap[adType]?.isAdAvailable() ?: false
    }

    override fun isOverLimitDay(): Boolean {
        return isOverDayLimit
    }
}