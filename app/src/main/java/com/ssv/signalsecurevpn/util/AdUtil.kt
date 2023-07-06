package com.ssv.signalsecurevpn.util

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle

object AdUtil {
    var isAdPageDestroyEvent = false
    fun activityIsResume(activity: AppCompatActivity): Boolean {
        if (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            return true
        }
        return false
    }

    fun pageIsCanJump(activity: AppCompatActivity): Boolean {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return true
        }
        return false
    }
}