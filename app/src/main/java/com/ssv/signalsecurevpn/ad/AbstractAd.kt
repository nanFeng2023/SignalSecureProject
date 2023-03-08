package com.ssv.signalsecurevpn.ad

import androidx.appcompat.app.AppCompatActivity

/*抽象广告类*/
abstract class AbstractAd {
    abstract fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?)
    abstract fun showAd(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?
    )

    abstract fun isAdAvailable(adType: String):Boolean
}