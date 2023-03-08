package com.ssv.signalsecurevpn.ad

import androidx.appcompat.app.AppCompatActivity

object AdManager {
    var abstractAd: AbstractAd? = null

    fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?) {
        abstractAd?.loadAd(type, adLoadStateCallBack)
    }

    fun showAd(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?
    ) {
        abstractAd?.showAd(activity, adType, adShowStateCallBack)
    }

    fun isAdAvailable(adType: String): Boolean? {
       return abstractAd?.isAdAvailable(adType)
    }
}