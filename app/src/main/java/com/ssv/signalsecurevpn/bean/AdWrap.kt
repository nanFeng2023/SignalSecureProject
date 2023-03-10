package com.ssv.signalsecurevpn.bean

import com.google.android.gms.ads.AdLoader
import com.ssv.signalsecurevpn.ad.AdLoadStateCallBack
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import java.util.*

class AdWrap(type: String) {
    var adType = type//广告类型
    var ad: Any? = null//广告位
    var expirationTime: Long? = 0//过期时间
    var isAdLoading = false
    var adLoadStateCallBack: AdLoadStateCallBack? = null
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {//判断广告是否过期
        val dateDifference: Long = Date().time - expirationTime!!
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(): Boolean {
        return ad != null && wasLoadTimeLessThanNHoursAgo(ConfigurationUtil.AD_EXPIRATION_TIME)
    }
}
