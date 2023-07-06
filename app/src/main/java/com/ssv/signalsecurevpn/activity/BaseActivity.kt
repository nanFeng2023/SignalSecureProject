package com.ssv.signalsecurevpn.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.ProjectUtil

abstract class BaseActivity : AppCompatActivity() {
    var isResume = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenAdaption()
        val view = View.inflate(this, getLayout(), null)
        setContentView(view)
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        view.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)


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

    override fun onResume() {
        super.onResume()
        isResume = true
    }

    override fun onPause() {
        super.onPause()
        isResume = false
    }

    fun setTopMargin(v: View) {
        //布局预留高度
        ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
            when (view.layoutParams) {
                is FrameLayout.LayoutParams -> {
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.topMargin = insets.systemWindowInsetTop
                }
                is ConstraintLayout.LayoutParams -> {
                    val params = view.layoutParams as ConstraintLayout.LayoutParams
                    params.topMargin = insets.systemWindowInsetTop
                }
                is LinearLayout.LayoutParams -> {
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.topMargin = insets.systemWindowInsetTop
                }
                is RelativeLayout.LayoutParams -> {
                    val params = view.layoutParams as RelativeLayout.LayoutParams
                    params.topMargin = insets.systemWindowInsetTop
                }
            }
            insets
        }
    }

}