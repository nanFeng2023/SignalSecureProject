package com.ssv.signalsecurevpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.secure.fast.signalvpn.R
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.ProjectUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

/*
* 启动页面
*/
class LaunchActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var canBack = true
    private var isRestrictCountry = false

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //设置singleTask后 需要重新设置新的intent数据才生效
        setIntent(intent)
    }

    override fun businessProcess() {
        Log.i("main", "LaunchActivity---onCreate")
        countDown(100, start = {
            canBack = false
//            detectionIp()//限制IP判断
        }, next = {
            progressBar.progress = it
        }, end = {
            val isHotLaunch = intent.getBooleanExtra(ProjectUtil.IS_HOT_LAUNCH_KEY, false)
            canBack = true
            if (isHotLaunch) {//热启动关闭闪屏页，恢复到之前页面
                finish()
                return@countDown
            }
            jumpActivity()
        })
    }

    override fun bindViewId() {
        progressBar = findViewById(R.id.progressbar_launch)
    }

    override fun getLayout(): Int {
        return R.layout.activity_launch
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {//屏蔽返回键
            return canBack
        }
        return super.onKeyDown(keyCode, event)
    }

    /*检测IP*/
    private fun detectionIp() {
        OkGo.get<String>(NetworkUtil.BASE_URL)
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
//                    response?.let { Log.i("TAG:onSuccess", it.body()) }
                    Log.i("TAG", "success")
                    response?.let { parseData(it.body()) }
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Log.i("TAG", "error")
                }
            })
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
                    delay(20)
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
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("isRestrictCountry", isRestrictCountry)
        startActivity(intent)
        finish()
    }

    private fun parseData(data: String) {
        var jsonObj: JSONObject? = null
        try {
            jsonObj = JSONObject(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val country = jsonObj?.opt("country")
        Log.i("TAG", "country:$country")
        isRestrictCountry = "HK" == country
    }
}