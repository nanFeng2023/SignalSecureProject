package com.testbird.signalsecurevpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.testbird.signalsecurevpn.util.AdConfigurationUtil
import com.testbird.signalsecurevpn.util.NetworkUtil
import com.testbird.signalsecurevpn.util.ProjectUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/*
* 启动页面
*/
class LaunchActivity : BaseActivity(), AdManager.OnAdStateListener {
    private lateinit var progressBar: ProgressBar
    private var canBack = true
    private var delayIntervalTime = 100L;//延时间隔时间  默认10s

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //设置singleTask后 需要重新设置新的intent数据才生效
        setIntent(intent)
    }

    override fun businessProcess() {
        Timber.tag(AdConfigurationUtil.LOG_TAG).d("LaunchActivity----businessProcess()")
        countDown(100, start = {
            canBack = false
            //限制IP判断
            NetworkUtil.detectionIp(null)
            //注册广告回调监听
            AdManager.onAdStateListener = this
            //请求广告
            AdManager.loadOpenAd()
        }, next = {
            //更新进度条
            progressBar.progress = it
        }, end = {
            canBack = true
            Timber.tag(AdConfigurationUtil.LOG_TAG)
                .d("LaunchActivity----动画加载完成,即将加载广告")
            AdManager.showOpenAd(this@LaunchActivity)
        })

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
                Log.e("TAG", it.message ?: "协程倒计时错误")
            }.collect {
                //更新ui
                next(time - it)
            }
        }
    }

    private fun jumpActivity() {
        val isHotLaunch = intent.getBooleanExtra(ProjectUtil.IS_HOT_LAUNCH_KEY, false)
        if (isHotLaunch) {//热启动关闭闪屏页，恢复到之前页面
            finish()
            Timber.tag(AdConfigurationUtil.LOG_TAG)
                .d("LaunchActivity----jumpActivity()--关闭页面显示之前页面")
        } else {
            Timber.tag(AdConfigurationUtil.LOG_TAG).d("LaunchActivity----jumpActivity()--跳转到主页")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onAdLoaded() {
        Timber.tag(AdConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoaded()")
        delayIntervalTime = 10L
    }

    override fun onAdLoadFail() {
        Timber.tag(AdConfigurationUtil.LOG_TAG).d("LaunchActivity----onAdLoadFail()")
        jumpActivity()
    }

    override fun onShowAdComplete() {
        Timber.tag(AdConfigurationUtil.LOG_TAG).d("LaunchActivity----onShowAdComplete()")
        jumpActivity()
    }

    override fun onDestroy() {
        super.onDestroy()
        //取消广告回调监听
        AdManager.onAdStateListener = null
    }
}