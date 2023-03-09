package com.ssv.signalsecurevpn

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.ProjectUtil

abstract class BaseActivity : AppCompatActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenAdaption()
        //沉浸式状态栏
        steepStatusBar()

        val view = View.inflate(this, getLayout(), null)
        setContentView(view)

        //禁止横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        //禁止键盘挤压布局
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        bindViewId()
        businessProcess()
        netCheck()
    }

    private fun screenAdaption() {
        val metrics: DisplayMetrics = resources.displayMetrics
        val td = metrics.heightPixels / 760f
        val dpi = (160 * td).toInt()
        metrics.density = td
        metrics.scaledDensity = td
        metrics.densityDpi = dpi
    }

    open fun netCheck(): Boolean {
        if (NetworkUtil.getNetworkType(this) == 0) {//无网络
            Toast.makeText(this, "Please check your network", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    abstract fun businessProcess()

    abstract fun bindViewId()

    abstract fun getLayout(): Int

    @SuppressLint("ObsoleteSdkInt")
    fun steepStatusBar() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = option
            window.statusBarColor = Color.TRANSPARENT

            ProjectUtil.setStateBarColor(this@BaseActivity)
        }
    }

    open fun View.click(listener: (view: View) -> Unit) {
        val minTime = 500L
        var lastTime = 0L
        this.setOnClickListener {
            val currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis - lastTime > minTime) {
                lastTime = currentTimeMillis
                listener.invoke(this)
            }
        }
    }

    inline fun View.setOnSingleClickListener(crossinline onClick: () -> Unit) {
        val minTime = 500L
        var lastTime = 0L
        this.setOnClickListener {
            val currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis - lastTime > minTime) {
                lastTime = currentTimeMillis
                onClick()
            }
        }
    }
}