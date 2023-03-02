package com.testbird.signalsecurevpn

import android.content.Context
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.ktx.remoteConfig


object FirebaseUtils {
    fun loadConfigure(context: Context){
        val remoteConfig = Firebase.remoteConfig
//        remoteConfig.fetchAndActivate().addOnCompleteListener(context, OnCompleteListener {  })
    }
}