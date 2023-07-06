package com.ssv.signalsecurevpn.bean

import com.github.shadowsocks.bg.BaseService
import com.ssv.signalsecurevpn.activity.MainActivity
import com.ssv.signalsecurevpn.ad.AdLoadStateCallBack
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.SharePreferenceUtil
import java.util.*

class AdWrap(type: String) {
    var adType = type//广告类型
    var ad: Any? = null//广告位
    var expirationTime: Long? = 0//过期时间
    var isAdLoading = false
    var adLoadStateCallBack: AdLoadStateCallBack? = null
    var isReload = true
    var adBeanList: ArrayList<AdBean>? = null
    var adBean: AdBean? = null
    var adSpaceName = ""
    var adLoadIp = ""
    var adLoadCity = ""

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {//判断广告是否过期
        val dateDifference: Long = Date().time - expirationTime!!
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(): Boolean {
        return ad != null && wasLoadTimeLessThanNHoursAgo(ConfigurationUtil.AD_EXPIRATION_TIME)
    }

    fun refreshAdSpace() {
        when (adType) {
            AdMob.AD_OPEN -> {
                adSpaceName = ConfigurationUtil.AD_SPACE_OPEN
            }
            AdMob.AD_NATIVE_HOME -> {
                adSpaceName = ConfigurationUtil.AD_SPACE_NATIVE_HOME
            }
            AdMob.AD_NATIVE_RESULT -> {
                adSpaceName = ConfigurationUtil.AD_SPACE_NATIVE_RESULT
            }
            AdMob.AD_INTER_CLICK -> {
                adSpaceName = ConfigurationUtil.AD_SPACE_INTER_CONNECTION
            }
            AdMob.AD_INTER_IB -> {
                adSpaceName = ConfigurationUtil.AD_SPACE_INTER_BACK
            }
        }
    }

    fun refreshAdLoadIpAndCity() {
        if (MainActivity.vpnState == BaseService.State.Connected) {
            adLoadIp = MainActivity.currentVpnBean?.ip ?: "none"
            adLoadCity = MainActivity.currentVpnBean?.city ?: "none"
        } else {
            val curIp = SharePreferenceUtil.getString(ConfigurationUtil.CUR_CONNECT_IP)
            adLoadIp = curIp.toString()
            adLoadCity = "none"
        }
    }

    fun refreshAdShowIpAndCity(reportEventBean: AdReportEventBean) {
        if (MainActivity.vpnState == BaseService.State.Connected) {
            reportEventBean.adShowIp = MainActivity.currentVpnBean?.ip ?: "none"
            reportEventBean.adShowCity = MainActivity.currentVpnBean?.city ?: "none"
        } else {
            val curIp = SharePreferenceUtil.getString(ConfigurationUtil.CUR_CONNECT_IP)
            reportEventBean.adShowIp = curIp.toString()
            reportEventBean.adShowCity = "none"
        }
    }


}
