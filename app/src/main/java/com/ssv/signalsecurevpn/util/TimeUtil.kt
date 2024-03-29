package com.ssv.signalsecurevpn.util

import android.os.Looper
import android.os.Handler
import com.ssv.signalsecurevpn.call.TimeDataCallBack

object TimeUtil {
    private var handler: Handler? = Handler(Looper.getMainLooper())
    private var millisecondsRecord = 0L
    private var startTime = 0L
    private var timeBuff = 0L
    var curConnectTime: String? = null
    var dataCallList: ArrayList<TimeDataCallBack> = ArrayList()
    var timeStamp = 0L

    private val runnable = object : Runnable {
        override fun run() {
            millisecondsRecord = System.currentTimeMillis() - startTime
            calculateTime()
            updateTime(curConnectTime!!)
            handler?.postDelayed(this, 1000)
        }
    }

    private fun calculateTime() {
        val accumulatedTime = timeBuff + millisecondsRecord
        val seconds = accumulatedTime / 1000 % 60
        val minutes = accumulatedTime / 1000 / 60 % 60
        val hours = accumulatedTime / 1000 / 60 / 24 % 24
        timeStamp = hours * 3600 + minutes * 60 + seconds
        curConnectTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateTime(time: String) {
        for (i in 0 until dataCallList.size) {
            dataCallList[i].onTime(curConnectTime!!)
        }
    }

    fun startAccumulateTime() {
        startTime = System.currentTimeMillis()
        handler?.postDelayed(runnable, 1000)
    }

    fun resetTime() {
        millisecondsRecord = 0L
        timeBuff = 0L
        calculateTime()
    }

    fun pauseTime() {
        timeBuff += millisecondsRecord
        handler?.removeCallbacks(runnable)
    }

}