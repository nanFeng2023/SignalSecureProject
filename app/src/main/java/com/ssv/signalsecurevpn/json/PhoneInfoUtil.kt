package com.ssv.signalsecurevpn.json

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.ssv.signalsecurevpn.activity.App
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.SharePreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URLEncoder
import java.util.*


object PhoneInfoUtil {
    @JvmStatic
    fun currentTimeZone(): String {
        return createGmtOffsetString(
            includeGmt = false,
            includeMinuteSeparator = false,
            offsetMillis = TimeZone.getDefault().rawOffset
        )
    }

    private fun createGmtOffsetString(
        includeGmt: Boolean,
        includeMinuteSeparator: Boolean,
        offsetMillis: Int
    ): String {
        var offsetMinutes = offsetMillis / 60000
        val builder: StringBuilder = StringBuilder(9)
        if (includeGmt) {
            var sign = '+'
            if (offsetMinutes < 0) {
                sign = '-'
                offsetMinutes = -offsetMinutes
            }
            builder.append("GMT")
            builder.append(sign)
        }
        appendNumber(builder, 2, offsetMinutes / 60)
        if (includeMinuteSeparator) {
            builder.append(':')
            appendNumber(builder, 2, offsetMinutes % 60)
        }
        return builder.toString()
    }

    private fun appendNumber(builder: StringBuilder, count: Int, value: Int) {
        val string = value.toString()
        for (i in 0 until count - string.length) {
            builder.append('0')
        }
        builder.append(string)
    }

    //获取网络运营商
    fun getCarrierName(): String {
        val simOperatorName = getSimOperatorName()
        return simOperatorName.ifEmpty {
            getNetWorkOperatorName()
        }
    }

    //获取上网卡运营商
    private fun getSimOperatorName(): String {
        val opeType: String
        val carrierName: String
        val tm = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simOperator = tm.simOperator

        if (tm.simState != TelephonyManager.SIM_STATE_READY) {
            when (tm.simState) {
                TelephonyManager.SIM_STATE_ABSENT -> {//1
                    Timber.d("getSimOperatorName()---No Sim card")
                }
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> {//2
                    Timber.d("getSimOperatorName()---The status of the Sim card is locked. The PIN is required to unlock the SIM card")
                }
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> {//3
                    Timber.d("getSimOperatorName()---The status of the Sim card is locked. The PUK is required to unlock the SIM card")
                }
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> {//4
                    Timber.d("getSimOperatorName()---Requires a network PIN code to unlock")
                }
                else -> {

                }
            }
            return ""
        }
        carrierName = tm.simOperatorName
        Timber.d("getSimOperatorName()---The obtained MCC+MNC is:$simOperator")
        Timber.d("getSimOperatorName()---The obtained carrier name is:${tm.simOperatorName}")
        opeType = when (simOperator) {
            "46001", "46006", "46009" -> {
                "China Unicom"
            }
            "46000", "46002", "46004", "46007" -> {
                "China Mobile"
            }
            "46003", "46005", "46011" -> {
                "China Telecom"
            }
            "46020" -> {
                "China Tietong"
            }
            else -> {
                "OTHER"
            }
        }
        Timber.d("getSimOperatorName()---The operator name of the artificial judgment is:$opeType")
        return carrierName
    }

    //获取拨号卡运营商
    private fun getNetWorkOperatorName(): String {
        val opeType: String
        val carrierName: String
        val tm = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        //用于判断拨号那张卡的运营商
        val networkOperator = tm.networkOperator
        Timber.d("getNetWorkOperatorName()---The obtained MCC+MNC is:$networkOperator")
        if (tm.phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            Timber.d("getNetWorkOperatorName()---On a CDMA network, it's not reliable, so you don't use this method")
            return ""
        }
        when (tm.phoneType) {
            0 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:NO_PHONE")
            }
            1 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:GSM_PHONE")
            }
            2 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:CDMA_PHONE")
            }
            3 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:SIP_PHONE")
            }
            4 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:THIRD_PARTY_PHONE")
            }
            5 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:IMS_PHONE")
            }
            6 -> {
                Timber.d("getNetWorkOperatorName()---The network type is:CDMA_LTE_PHONE")
            }
        }
        carrierName = tm.networkOperatorName
        Timber.d("getNetWorkOperatorName()---The obtained network type name is:$${tm.networkOperatorName}")
        opeType =
            when (networkOperator) {
                "46001", "46006", "46009" -> {
                    "China Unicom"
                }
                "46000", "46002", "46004", "46007" -> {
                    "China Mobile"
                }
                "46003", "46005", "46011" -> {
                    "China Telecom"
                }
                "46020" -> {
                    "China Tietong"
                }
                else -> {
                    "OTHER"
                }
            }
        Timber.d("getNetWorkOperatorName()---The operator name of the artificial judgment is:$opeType")
        return carrierName
    }

    fun curCountry(): String {//header中要求去除分号
        return Locale.getDefault().country.replace(";", "")
    }

    fun getSysLanguage(): String {
        val language = Locale.getDefault().language
        val country = Locale.getDefault().country
        return buildString {
            append(language).append("_").append(country)
        }
    }

    fun getUUID(): String {
        val randomUUID = UUID.randomUUID()
        return randomUUID.toString()
    }

    //没有网络连接
    private const val NETWORK_NONE = 0

    //wifi连接
    private const val NETWORK_WIFI = 1

    //手机网络数据连接类型
    private const val NETWORK_2G = 2
    private const val NETWORK_3G = 3
    private const val NETWORK_4G = 4
    private const val NETWORK_5G = 5
    private const val NETWORK_MOBILE = 6

    var isLoadingRemoteSerData = false

    fun netWorkTypeStr(): String {
        when (getNetworkState()) {
            NETWORK_NONE -> {
                return "no network"
            }
            NETWORK_WIFI -> {
                return "wifi"
            }
            NETWORK_2G -> {
                return "2g"
            }
            NETWORK_3G -> {
                return "3g"
            }
            NETWORK_4G -> {
                return "4g"
            }
            NETWORK_5G -> {
                return "5g"
            }
            NETWORK_MOBILE -> {
                return "mobile"
            }
        }
        return "no network"
    }

    /**
     * 获取当前网络连接类型
     */
    private fun getNetworkState(): Int {
        val connectivityManager =
            App.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable)
            return NETWORK_NONE
        val wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiInfo != null) {
            val state = wifiInfo.state
            if (state != null) {
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    return NETWORK_WIFI
                }

            }
        }
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        if (networkInfo != null) {
            val state = networkInfo.state
            val subtypeName = networkInfo.subtypeName
            if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                when (activeNetworkInfo.subtype) {
                    //如果是2g类型
                    TelephonyManager.NETWORK_TYPE_GPRS,//联通2g
                    TelephonyManager.NETWORK_TYPE_CDMA,//电信2g
                    TelephonyManager.NETWORK_TYPE_EDGE,//移动2g
                    TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyManager.NETWORK_TYPE_IDEN -> {
                        return NETWORK_2G
                    }
                    //如果是3g类型
                    TelephonyManager.NETWORK_TYPE_EVDO_A,// 电信3g
                    TelephonyManager.NETWORK_TYPE_UMTS,
                    TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_EVDO_B,
                    TelephonyManager.NETWORK_TYPE_EHRPD,
                    TelephonyManager.NETWORK_TYPE_HSPAP -> {
                        return NETWORK_3G
                    }
                    //如果是4g类型
                    TelephonyManager.NETWORK_TYPE_LTE -> {
                        return NETWORK_4G
                    }
                    TelephonyManager.NETWORK_TYPE_NR -> {//对应的20 只有依赖为android 10.0才有此属性
                        return NETWORK_5G
                    }
                    else -> {
                        //中国移动 联通 电信 三种3G制式
                        return if (subtypeName.equals("TD-SCDMA", ignoreCase = true)
                            || subtypeName.equals("WCDMA", ignoreCase = true)
                            || subtypeName.equals("CDMA2000", ignoreCase = true)
                        ) {
                            NETWORK_3G;
                        } else {
                            NETWORK_MOBILE;
                        }
                    }

                }
            }
        }
        return NETWORK_NONE
    }

    fun getCpu(): String {
        return Build.SUPPORTED_ABIS[0]
    }

    fun getAndroidId(): String {
        var androidId = SharePreferenceUtil.getString(ConfigurationUtil.ANDROID_ID)
        if (!androidId.isNullOrEmpty())
            return androidId
        val androidID = Settings.System.getString(
            App.appContext.contentResolver, Settings.Secure.ANDROID_ID
        )
        androidId = androidID.ifEmpty {
            getUUID()
        }
        SharePreferenceUtil.putString(ConfigurationUtil.ANDROID_ID, androidId)
        return androidId
    }

    fun appVersionName(): String {
        val packageManager = App.appContext.packageManager
        val packageInfo = packageManager.getPackageInfo(App.appContext.packageName, 0)
        return packageInfo.versionName
    }

    //1.获取内存可用大小,内存路径
    var path: String = Environment.getDataDirectory().absolutePath
    var memorySpace: String =
        android.text.format.Formatter.formatFileSize(App.appContext, getTotalSpace(path))

    fun getStorageSize(): String {
        return URLEncoder.encode(memorySpace.replace(" ", ""), "UTF-8")
    }

    private fun getTotalSpace(path: String): Long {
        //获取总内存大小
        val statFs = StatFs(path)
        //获取总区块的个数
        val count = statFs.blockCountLong
        //获取区块大小
        val size = statFs.blockSize.toLong()
        //总大小
        return count * size
    }

    fun isOpenLimitAdTrack(activity: FragmentActivity) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                AdvertisingIdClient.getAdvertisingIdInfo(App.appContext).apply {
                    val str = if (isLimitAdTrackingEnabled)
                        "madman"
                    else "codicil"
                    Timber.d("isOpenLimitAdTrack()---str:$str")
                    SharePreferenceUtil.putString(ConfigurationUtil.LIMIT_TRACK, str)
                    id?.let { SharePreferenceUtil.putString(ConfigurationUtil.GAID, it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAppFirstInstallTime(): Long {
        val packageInfo =
            App.appContext.packageManager.getPackageInfo(App.appContext.packageName, 0)
        return packageInfo.firstInstallTime
    }

    fun getAppLastRefreshTime(): Long {
        val packageInfo =
            App.appContext.packageManager.getPackageInfo(App.appContext.packageName, 0)
        return packageInfo.lastUpdateTime
    }

    fun getBattery(): Long {
        val batteryManager = App.appContext.getSystemService(BATTERY_SERVICE) as BatteryManager?
        val batteryCapacity =
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batteryCapacity?.toLong() ?: 0
    }

}