package com.ssv.signalsecurevpn.ad

interface AdShowStateCallBack {
    fun onAdDismiss()
    fun onAdShowed()
    fun onAdShowFail()
    fun onAdClicked()
}