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
                val sigvn_ad = remoteConfig.getString("sigvn_ad")
                SharePreferenceUtil.putString(AdMob.SIGVN_AD, sigvn_ad)
            }
        }
    }
}
