package com.ssv.signalsecurevpn.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.ParseException
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.activity.App
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/*帮助类*/
object ProjectUtil {
    //常量传值key
    const val CUR_SELECT_CITY = "curSelectCity"
    const val SAVE_TIME_MILLIS = "saveTimeMillis"
    const val COUNTRY_KEY = "country"
    const val DEFAULT_FAST_SERVERS = "Fast Servers"
    const val IS_FIRST_INTO_APP = "isFirstIntoApp"
    const val CUR_SELECT_SERVICE_POSITION = "curSelectServicePosition"
    const val LAST_SELECT_CITY = "lastSelectCity"
    const val AD_CLICK_NUM = "adClickNum"
    const val AD_SHOW_NUM = "adShowNum"
    const val LAST_AD_CLICK_TIME = "lastAdClickTime"
    const val LAST_AD_SHOW_TIME = "lastAdShowTime"

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
        val countryIconResourceId = when (countryName.lowercase()) {
            DEFAULT_FAST_SERVERS -> R.mipmap.ic_default_country
            "united states" -> R.mipmap.ic_united_states
            "canada" -> R.mipmap.ic_canada
            "australia" -> R.mipmap.ic_australia
            "belgium" -> R.mipmap.ic_belgium
            "brazil" -> R.mipmap.ic_brazil
            "united kingdom" -> R.mipmap.ic_united_kingdom
            "france" -> R.mipmap.ic_france
            "germany" -> R.mipmap.ic_germany
            "hong kong" -> R.mipmap.ic_hong_kong
            "india" -> R.mipmap.ic_india
            "israel" -> R.mipmap.ic_israel
            "italy" -> R.mipmap.ic_italy
            "japan" -> R.mipmap.ic_japan
            "south korea" -> R.mipmap.ic_south_korea
            "netherlands" -> R.mipmap.ic_netherlands
            "new zealand" -> R.mipmap.ic_new_zealand
            "norway" -> R.mipmap.ic_norway
            "ireland" -> R.mipmap.ic_ireland
            "russian federation" -> R.mipmap.ic_russian_federation
            "singapore" -> R.mipmap.ic_singapore
            "sweden" -> R.mipmap.ic_sweden
            "switzerland" -> R.mipmap.ic_switzerland
            "taiwan" -> R.mipmap.ic_taiwan
            "turkey" -> R.mipmap.ic_turkey
            "united arab emirates" -> R.mipmap.ic_united_arab_emirates
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


    fun callEmail(addresses: Array<String>, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$addresses")
            intent.putExtra(Intent.EXTRA_EMAIL, addresses)
            val chooserIntent = Intent.createChooser(intent, "Select email")
            if (chooserIntent != null) {
                context.startActivity(chooserIntent);
            } else {
                Toast.makeText(App.appContext, "Please set up a Mail account", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(App.appContext, "Please set up a Mail account", Toast.LENGTH_SHORT).show()
        }
    }

    fun callShare(context: Context) {//分享文本
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            "${ConfigurationUtil.GOOGLE_STORE_URL}${context.packageName}"
        )
        context.startActivity(Intent.createChooser(shareIntent, "share"))
    }

    fun openGooglePlay() {
        val playPackage = "com.android.vending"
        val packageName = App.appContext.packageName
        try {
            val parse = Uri.parse("market://details?id=${packageName}")
            val intent = Intent(Intent.ACTION_VIEW, parse)
            intent.`package` = playPackage
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.appContext.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            val parse =
                Uri.parse("${ConfigurationUtil.GOOGLE_STORE_URL}$packageName")
            val intent = Intent(Intent.ACTION_VIEW, parse)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.appContext.startActivity(intent)
        }
    }


    fun setStateBarColor(activity: Activity) {
        val decorView: View = activity.window.decorView
        decorView.post {
            val colorCount = 5
            val rect = Rect()

            decorView.getWindowVisibleDisplayFrame(rect)
            decorView.isDrawingCacheEnabled = true
            decorView.buildDrawingCache()
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
        return city?.split("-")?.get(0) ?: DEFAULT_FAST_SERVERS
    }


    fun longToYMDHMS(long: Long): String {
        val date = Date(long)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return simpleDateFormat.format(date)
    }

    /**
     * 判断给定字符串时间是否为今日
     * @param date
     * @return boolean
     */
    fun isToday(date: String?): Boolean {
        var isToday = false
        val time: Date? = toDate(date)
        val today = Date()
        if (time != null) {
            val nowDate: String = dateFormat2.get()?.format(today) ?: ""
            val timeDate: String = dateFormat2.get()?.format(time) ?: ""
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("ProjectUtil----isToday()---nowDate:$nowDate---timeDate:$timeDate")
            if (nowDate == timeDate) {
                isToday = true
            }
        }
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("ProjectUtil----isToday()---isToday:$isToday")
        return isToday
    }

    /**
     * 将字符串转位日期类型
     * @param date
     * @return
     */
    private fun toDate(date: String?): Date? {
        return try {
            date?.let { dateFormat.get()?.parse(it) }
        } catch (e: ParseException) {
            null
        }
    }

    private val dateFormat: ThreadLocal<SimpleDateFormat?> =
        object : ThreadLocal<SimpleDateFormat?>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            }
        }

    private val dateFormat2: ThreadLocal<SimpleDateFormat?> =
        object : ThreadLocal<SimpleDateFormat?>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd")
            }
        }
}