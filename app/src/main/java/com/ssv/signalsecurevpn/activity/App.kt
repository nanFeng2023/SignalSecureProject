package com.ssv.signalsecurevpn.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.github.shadowsocks.Core
import com.google.android.gms.ads.MobileAds
import com.google.firebase.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheEntity
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.model.HttpHeaders
import com.lzy.okgo.model.HttpParams
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.util.FirebaseUtils
import com.ssv.signalsecurevpn.util.PlanUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

class App : MultiDexApplication() {
    companion object {
        lateinit var appContext: Application
        var isColdLaunch: Boolean = true//第一次或杀掉进程再进都是冷启动
    }

    init {
        appContext = this
    }

    override fun onCreate() {
        super.onCreate()
        //服务要在初始化在子进程，不能进行主进程判断后去初始化
        Core.init(this, MainActivity::class)
        if (isMainProcess()) {
            Firebase.initialize(this)
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            //初始化okGo
            initReadyOkGo()
            //注册生命周期监听所有页面的返回或home键实现热启动 引导到主页面
            registerActivityLifecycleCallbacks(CustomActivityLifecycleCallback)
            //广告注册
            MobileAds.initialize(this) {}
            Core.stopService()//杀掉主进程重启关闭VPN
            //注册AdMob
            AdManager.abstractAd = AdMob
            //异步初始化
            asyInit()
        }
    }

    private fun asyInit() {
        GlobalScope.launch(Dispatchers.IO) {
            //日志开关
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
            //远端请求服务器和广告配置数据
            if (!BuildConfig.DEBUG) {
                FirebaseUtils.loadConfigure()
            }
            PlanUtil.obtainReferrer()
        }
    }

    private fun isMainProcess(): Boolean {
        return appContext.packageName.equals(getCurProcessName())
    }

    @SuppressLint("ServiceCast")
    private fun getCurProcessName(): String? {
        val myPid = android.os.Process.myPid()
        val activityManager: ActivityManager =
            appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        for (runningAppProcess in runningAppProcesses) {
            if (runningAppProcess.pid == myPid) {
                return runningAppProcess.processName
            }
        }
        return null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }


    private fun initReadyOkGo() {
        val headers = HttpHeaders()
        val params = HttpParams()
        val builder = OkHttpClient.Builder()
        //超时时间默认60秒
        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS) //读取超时时间
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS) //写入超时时间
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS) //连接超时时间

        OkGo.getInstance().init(this) //初始化
            .setOkHttpClient(builder.build())
            .setCacheMode(CacheMode.NO_CACHE) //全局统一缓存模式
            .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE) //全局统一缓存时间，
            .setRetryCount(1) //全局统一超时重连次数
            .addCommonHeaders(headers) //公共头
            .addCommonParams(params) //公共参数
    }

}