package com.ssv.signalsecurevpn.util

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.ssv.signalsecurevpn.ad.AdMob

/*远端获取数据*/
object FirebaseUtils {
    fun loadConfigure() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val adData = remoteConfig.getString("sigvn_ad")
                SharePreferenceUtil.putString(AdMob.SIGVN_AD, adData)
            }
        }
    }
}
