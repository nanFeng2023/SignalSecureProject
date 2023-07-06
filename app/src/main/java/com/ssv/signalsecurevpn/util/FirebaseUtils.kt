package com.ssv.signalsecurevpn.util

import android.os.Bundle
import com.github.shadowsocks.core.BuildConfig
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.json.EventJson

/*远端获取数据*/
object FirebaseUtils {
    var dataLoadFinished = false

    fun loadConfigure() {
        try {
            dataLoadFinished = false
            val remoteConfig = Firebase.remoteConfig
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                if (it.isSuccessful) {
                    val adData = remoteConfig.getString(ConfigurationUtil.REMOTE_AD_KEY)
                    SharePreferenceUtil.putString(ConfigurationUtil.REMOTE_AD_KEY, adData)

                    val planStr = remoteConfig.getString(ConfigurationUtil.REMOTE_PLAN_KEY)
                    SharePreferenceUtil.putString(ConfigurationUtil.REMOTE_PLAN_KEY, planStr)
                    dataLoadFinished = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun upLoadLogEvent(event: String, bundle: Bundle? = null) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(event, bundle)
        }
        EventJson.uploadDotEventJson(event,bundle)
    }
}
