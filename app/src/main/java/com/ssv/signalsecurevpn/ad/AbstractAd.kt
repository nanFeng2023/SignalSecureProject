package com.ssv.signalsecurevpn.ad

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/*抽象广告类*/
abstract class AbstractAd {
    abstract fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?)
    abstract fun showAd(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?,
        layoutId: Int?=0,
        nativeAdParentGroup: ViewGroup? = null
    )

    abstract fun isAdAvailable(adType: String): Boolean
}