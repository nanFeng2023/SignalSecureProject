package com.ssv.signalsecurevpn.ad

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

object AdManager {
    var abstractAd: AbstractAd? = null

    fun loadAd(type: String, adLoadStateCallBack: AdLoadStateCallBack?) {
        abstractAd?.loadAd(type, adLoadStateCallBack)
    }

    fun showAd(
        activity: AppCompatActivity,
        adType: String,
        adShowStateCallBack: AdShowStateCallBack?,
        layoutId: Int?=0,
        nativeAdParentGroup: ViewGroup?=null
    ) {
        abstractAd?.showAd(activity, adType, adShowStateCallBack,layoutId,nativeAdParentGroup)
    }

    fun isAdAvailable(adType: String): Boolean? {
       return abstractAd?.isAdAvailable(adType)
    }
}