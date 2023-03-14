package com.ssv.signalsecurevpn.util

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle

object AdUtil {
    fun activityIsResume(activity: AppCompatActivity): Boolean {
        if (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            return true
        }
        return false
    }
}