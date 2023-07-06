package com.ssv.signalsecurevpn.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.ad.AdLoadStateCallBack
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.ad.AdShowStateCallBack
import com.ssv.signalsecurevpn.json.PhoneInfoUtil
import com.ssv.signalsecurevpn.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/*
* 启动页面
*/
class LaunchActivity : BaseActivity(), AdLoadStateCallBack {
    private lateinit var progressBar: ProgressBar
    private var canBack = true
    private var delayIntervalTime = 100L
    private var isAdLoadFinished = false

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun businessProcess() {
        PhoneInfoUtil.isOpenLimitAdTrack(this)
        initProgressJob()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canBack) {
                    finish()
                }
            }
        })
        FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_LAUNCH_SHOW)
        NetworkUtil.requestServerData()
        NetworkUtil.obtainServiceData(SharePreferenceUtil.getString(ConfigurationUtil.REMOTE_SERVER_DATA))
    }

    private fun initProgressJob() {
        countDown(start = {
            canBack = false
            //限制IP判断
            NetworkUtil.detectionIp(null)
            AdMob.isRefreshNativeAd = true
            isAdLoadFinished = false
            delayIntervalTime = 100L
        }, next = {
            updateProgress(it)
        }, end = {
            canBack = true
            AdManager.showAd(this@LaunchActivity, AdMob.AD_OPEN, object : AdShowStateCallBack {
                override fun onAdDismiss() {
                    Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdDismiss()")
                    jumpActivity()
                }

                override fun onAdShowed() {

                }

                override fun onAdShowFail() {
                    Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdShowFail()")
                    jumpActivity()
                }

                override fun onAdClicked() {

                }
            })
        })
    }

    private fun updateProgress(progress: Int) {
        progressBar.progress = progress
        if (FirebaseUtils.dataLoadFinished) {
            reqAd()
        } else {
            if (progress <= 40) {
                if (FirebaseUtils.dataLoadFinished) {
                    reqAd()
                }
            } else {
                reqAd()
            }
        }
        if (AdManager.isAdAvailable(AdMob.AD_OPEN) == true) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("LaunchActivity----updateProgress()---open have cache")
            delayIntervalTime = 10L
        }
    }

    private fun reqAd() {
        if (!isAdLoadFinished) {
            //请求广告数据，预加载
            NetworkUtil.obtainAdData()
            //请求方案
            NetworkUtil.obtainPlanData()

            //请求开屏广告
            AdManager.loadAd(AdMob.AD_OPEN, this)
            //预加载插屏
            AdManager.loadAd(AdMob.AD_INTER_CLICK, null)
            //预加载原生广告home
            AdManager.loadAd(AdMob.AD_NATIVE_HOME, null)
            //预加载原生广告result
            AdManager.loadAd(AdMob.AD_NATIVE_RESULT, null)
            isAdLoadFinished = true
        }

    }

    override fun bindViewId() {
        progressBar = findViewById(R.id.progressbar_launch)
    }

    override fun getLayout(): Int {
        return R.layout.activity_launch
    }

    private fun AppCompatActivity.countDown(
        start: (scope: CoroutineScope) -> Unit,
        next: (time: Int) -> Unit,
        end: () -> Unit
    ) {
        lifecycleScope.launch {
            flow {
                (0 until 100).forEach {
                    delay(delayIntervalTime)
                    emit(it)
                }
            }.onStart {
                start(this@launch)
            }.onCompletion {
                end()
            }.collect {
                next(it)
            }
        }
    }

    private fun jumpActivity() {
        if (AdUtil.pageIsCanJump(this)) {
            Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----jumpActivity()--跳转到主页")
            val intent = Intent(this@LaunchActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("LaunchActivity----jumpActivity()--page can not jump")
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (AdUtil.isAdPageDestroyEvent) {
            AdUtil.isAdPageDestroyEvent = false
            initProgressJob()
        } else if (canBack) {
            initProgressJob()
        }
    }

    override fun onAdLoaded() {
        delayIntervalTime = 10L
        Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoaded()")
    }

    override fun onAdLoadFail() {
        delayIntervalTime = 10L
        Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoadFail()")
    }
}