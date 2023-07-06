package com.ssv.signalsecurevpn.activity

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdActivity
import com.ssv.signalsecurevpn.call.FrontAndBackgroundCallBack
import com.ssv.signalsecurevpn.util.AdUtil
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.ProjectUtil
import timber.log.Timber

object CustomActivityLifecycleCallback : ActivityLifecycleCallbacks {
    private var finalCount: Int = 0
    var frontAndBackgroundCallBack: FrontAndBackgroundCallBack? = null
    private var saveTimeMillis = -1L
    private val handler: Handler = Handler(Looper.getMainLooper())
    override fun onActivityCreated(activity: Activity, p1: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        finalCount++
        //如果finalCount==1,说明应用是后台到前台
        if (finalCount == 1) {
            val currentTimeMillis = System.currentTimeMillis()
            val timeMillis = currentTimeMillis - saveTimeMillis
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("ColdActivityLifecycleCallback----onActivityStarted()---activity:${activity.localClassName},timeMillis:$timeMillis")
            handler.removeCallbacksAndMessages(null)
            if (saveTimeMillis != -1L && timeMillis > 3000 && !ProjectUtil.isAppMainBack) {//超过3s并且是热启动,并且不是主页返回键再进来
                // ，走热启动流程
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("ColdActivityLifecycleCallback----onActivityStarted()---热启动")
                val intent = Intent(activity, LaunchActivity::class.java)
                activity.startActivity(intent)
            }
            frontAndBackgroundCallBack?.onAppToFront()
        }
    }

    override fun onActivityResumed(p0: Activity) {

    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {
        finalCount--
        //finalCount==0,说明应用前台到后台
        if (finalCount == 0) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("ColdActivityLifecycleCallback----onActivityStopped()---activity:${activity.localClassName}")
            saveTimeMillis = System.currentTimeMillis()
            frontAndBackgroundCallBack?.onAppToBackGround()
            judgeAdActivity(activity)
        }
    }

    private fun judgeAdActivity(activity: Activity) {
        if (activity is AdActivity) {
            handler.postDelayed({
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("ColdActivityLifecycleCallback----onActivityStopped()---judgeAdActivity()---activity:${activity.localClassName}")
                activity.finish()
                AdUtil.isAdPageDestroyEvent = true
            }, 3000)
        }
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}