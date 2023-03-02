package com.testbird.signalsecurevpn.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.testbird.signalsecurevpn.R
import java.io.File

/*帮助类*/
object ProjectUtil {
    //常量传值key
    const val TRANSFER_KEY = "vpnBean"
    const val CUR_SELECT_CITY = "curSelectCity"
    const val SAVE_TIME_MILLIS = "saveTimeMillis"
    const val IS_HOT_LAUNCH_KEY = "isHotLaunch"
    const val COUNTRY_KEY = "country"
    const val DEFAULT_FAST_SERVERS = "Fast Servers"
    const val IS_FIRST_INTO_APP = "isFirstIntoApp"
    const val CUR_SELECT_SERVICE_POSITION = "curSelectServicePosition"
    const val LAST_SELECT_CITY = "lastSelectCity"

    //------以下标志控制动画的状态，不用VPN返回状态是因为动画要延时2s后才去请求VPN-----------
    //vpn未连接
    var idle = true

    //vpn连接成功
    var connected = false

    //VPN连接中 屏蔽按钮点击 back事件
    var connecting = false

    //VPN关闭中 点击其他按钮 中断断开流程
    var stopping = false

    //vpn连接关闭
    var stopped = false
    //------------------------------------------------------------------------------

    //是否显示引导页
    var isShowGuide = false

    //app回到前台
    var appToFront = false

    //app回到后台
    var appToBackground = false

    //app 首页返回键返回
    var isAppMainBack = false

    //是否VPN服务器列表页面请求关闭连接
    var isVpnSelectPageReqStopVpn = false

    //国家icon资源选择
    fun selectCountryIcon(countryName: String): Int {
        val countryIconResourceId = when (countryName) {
            DEFAULT_FAST_SERVERS -> R.mipmap.ic_default_country
            "Unite State" -> R.mipmap.ic_united_states
            "Canada" -> R.mipmap.ic_canada
            "Australia" -> R.mipmap.ic_australia
            "Belgium" -> R.mipmap.ic_belgium
            "Brazil" -> R.mipmap.ic_brazil
            else -> {
                R.mipmap.ic_default_country
            }
        }
        return countryIconResourceId
    }

    fun callEmail(context: Context, emailText: String) {
        val uri = Uri.parse("mailto:$emailText")
        val queryIntentActivities =
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), 0)
        val packageList = ArrayList<String>()
        val emailIntents = ArrayList<Intent>()
        for (i in 0 until queryIntentActivities.size) {
            val packageName = queryIntentActivities[i].activityInfo.packageName
            if (packageList.contains(packageName)) {
                packageList.remove(packageName)
            }
            packageList.add(packageName)
        }

        for (i in 0 until packageList.size) {
            val launchIntentForPackage =
                context.packageManager.getLaunchIntentForPackage(packageList[i])
            launchIntentForPackage?.let { emailIntents.add(it) }
        }

        val chooserIntent = Intent.createChooser(emailIntents.removeAt(0), "Select app!")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            emailIntents.toArray(arrayOf<Parcelable>())
        )
        context.startActivity(chooserIntent)
    }


    fun callEmail(addresses: Array<String>, subject: String, context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        context.startActivity(intent)

//        val intent = Intent(Intent.ACTION_SENDTO)
//        intent.data = Uri.parse("mailto:$addresses")
////        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
//        intent.putExtra(Intent.EXTRA_SUBJECT, "标题")
//        intent.putExtra(Intent.EXTRA_TEXT, "内容")
//        context.startActivity(intent)
    }


    fun callShare(context: Context) {//分享文本
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "链接")
        context.startActivity(Intent.createChooser(shareIntent, "分享"))
    }

    fun openGooglePlay(context: Context) {
        val playPackage = "com.android.vending"
        val packageName = context.packageName
        try {
            val parse = Uri.parse("market://details?id=${packageName}")
            val intent = Intent(Intent.ACTION_VIEW, parse)
            intent.`package` = playPackage
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            val parse =
                Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
            val intent = Intent(Intent.ACTION_VIEW, parse)
            intent.`package` = playPackage
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }


    /**
     * 获取手机厂商
     *
     * @return 手机厂商
     */
    fun getDeviceBrand(): String? {
        Log.e("--获取手机厂商--:", Build.BRAND)
        return Build.BRAND
    }


    fun setStateBarColor(activity: Activity) {
        val decorView: View = activity.window.decorView
        decorView.post {
            val colorCount = 5
            val rect = Rect()

            decorView.getWindowVisibleDisplayFrame(rect)
            decorView.isDrawingCacheEnabled = true
            decorView.buildDrawingCache()
            if (rect.width() <= 0 || rect.top <= 0)//防止黑屏时候空指针
                return@post
            val bitmap: Bitmap = Bitmap.createBitmap(
                decorView.drawingCache, 0, 0, rect.width(), rect.top
            )
            Palette.from(bitmap)
                .maximumColorCount(colorCount)
                .setRegion(0, 0, rect.width(), rect.top)
                .generate {
                    it?.let { palette ->
                        var mostPopularSwatch: Palette.Swatch? = null
                        for (swatch in palette.swatches) {
                            if (mostPopularSwatch == null
                                || swatch.population > mostPopularSwatch.population
                            ) {
                                mostPopularSwatch = swatch
                            }
                        }
                        mostPopularSwatch?.let { swatch ->
                            val luminance = ColorUtils.calculateLuminance(swatch.rgb)
                            // 当luminance小于0.5时，我们认为这是一个深色值.
                            if (luminance < 0.55) {
                                setDarkStatusBar(activity)
                            } else {
                                setLightStatusBar(activity)
                            }
                        }
                    }
                }
            decorView.destroyDrawingCache()
        }
    }

    fun setLightStatusBar(activity: Activity) {
        val flags = activity.window.decorView.systemUiVisibility
        activity.window.decorView.systemUiVisibility =
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    fun setDarkStatusBar(activity: Activity) {
        val flags =
            activity.window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        activity.window.decorView.systemUiVisibility =
            flags xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    fun splitStrGetCountryName(city: String?): String {
        Log.i("TAG", "splitStrGetCountryName（）----上次选中的国家:$city")
        return city?.split("-")?.get(0) ?: DEFAULT_FAST_SERVERS
    }


}