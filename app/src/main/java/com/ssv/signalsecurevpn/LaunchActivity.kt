package com.ssv.signalsecurevpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.ssv.signalsecurevpn.ad.AdLoadStateCallBack
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.ad.AdShowStateCallBack
import com.ssv.signalsecurevpn.util.AdUtil
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.ProjectUtil
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
    private var delayIntervalTime = 100L;//延时间隔时间  默认10s
    private var isHotLaunch = false
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //设置singleTask后 需要重新设置新的intent数据才生效
        setIntent(intent)
    }

    override fun businessProcess() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----businessProcess()")
        countDown(100, start = {
            canBack = false
            //限制IP判断
            NetworkUtil.detectionIp(null)
            AdMob.isRefreshNativeAd = true
            isHotLaunch = intent.getBooleanExtra(ProjectUtil.IS_HOT_LAUNCH_KEY, false)
            if (isHotLaunch) {//热启动
                if (!AdManager.isAdAvailable(AdMob.AD_OPEN)!!) {//没有缓存或过期，重新请求
                    reqAd()
                }
            } else {//冷启动
                reqAd()
            }
        }, next = {
            //更新进度条
            progressBar.progress = it
        }, end = {
            canBack = true
            if (AdMob.isReqInterrupt) {//广告达到日上限
                jumpActivity()
            } else {
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .d("LaunchActivity----动画加载完成,即将加载广告")
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
            }
        })
    }

    private fun reqAd() {
        //请求开屏广告
        AdManager.loadAd(AdMob.AD_OPEN, this)
        //预加载插屏广告2
        AdManager.loadAd(AdMob.AD_INTER_IB, null)
        //预加载插屏广告1
        AdManager.loadAd(AdMob.AD_INTER_CLICK, null)
        //预加载原生广告home
        AdManager.loadAd(AdMob.AD_NATIVE_HOME, null)
        //预加载原生广告result
        AdManager.loadAd(AdMob.AD_NATIVE_RESULT, null)
    }

    override fun bindViewId() {
        progressBar = findViewById(R.id.progressbar_launch)
    }

    override fun getLayout(): Int {
        return R.layout.activity_launch
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && canBack) {//屏蔽返回键
            return super.onKeyDown(keyCode, event)
        }
        return true
    }


    //协程
    private fun AppCompatActivity.countDown(
        time: Int,
        start: (scope: CoroutineScope) -> Unit,
        next: (time: Int) -> Unit,
        end: () -> Unit
    ) {
        lifecycleScope.launch {
            flow {
                (time downTo 0).forEach {
                    delay(delayIntervalTime)
                    emit(it)
                }
            }.onStart {
                //倒计时开始
                start(this@launch)
            }.onCompletion {
                //倒计时结束
                end()
            }.catch {
                //错误
                Timber.tag(ConfigurationUtil.LOG_TAG)
                    .e("LaunchActivity----countDown()--协程倒计时错误:${it.message} ")
            }.collect {
                //更新ui
                next(time - it)
            }
        }
    }

    private fun jumpActivity() {
        if (isHotLaunch) {//热启动关闭闪屏页，恢复到之前页面
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("LaunchActivity----jumpActivity()--关闭页面显示之前页面")
            finish()
        } else {
            Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----jumpActivity()--跳转到主页")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onAdLoaded() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoaded()")
        delayIntervalTime = 10L
    }

    override fun onAdLoadFail() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoadFail()")
        jumpActivity()
    }
}