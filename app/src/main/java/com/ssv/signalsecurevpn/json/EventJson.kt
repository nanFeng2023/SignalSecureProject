package com.ssv.signalsecurevpn.json

import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.ssv.signalsecurevpn.activity.App
import com.ssv.signalsecurevpn.bean.AdReportEventBean
import com.ssv.signalsecurevpn.bean.AdWrap
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.SharePreferenceUtil
import org.json.JSONObject

object EventJson {
    fun uploadDotEventJson(eventName: String, bundle: Bundle? = null) {
        val commonEventJson = createCommonEventJson()
        commonEventJson.put("mousy", eventName)
        if (bundle != null) {
            commonEventJson.put("time&dainty", bundle.getLong("time"))
        }
        NetworkUtil.reportEvent(NetworkUtil.EventType.DOT_EVENT, commonEventJson)
    }

    private fun createCommonEventJson(): JSONObject {
        val json = JSONObject()
        val livenJson = JSONObject()
        //客户端时区
        livenJson.put("immerse", PhoneInfoUtil.currentTimeZone().toInt())
        //品牌
        livenJson.put("wipe", Build.BRAND)
        //操作系统版本号
        livenJson.put("evelyn", Build.VERSION.RELEASE)
        //网络供应商名称，mcc和mnc
        livenJson.put("winslow", PhoneInfoUtil.getCarrierName())
        //idfa 原值（iOS）
        livenJson.put("reach", "")
        //日志发生的客户端时间，毫秒数
        livenJson.put("bone", System.currentTimeMillis())
        //操作系统中的国家简写，例如 CN，US等
        livenJson.put("dunham", PhoneInfoUtil.curCountry())
        //每组实验编号
        livenJson.put("defector", "")
        //system language
        livenJson.put("stopover", PhoneInfoUtil.getSysLanguage())
        //ios的idfv原值
        livenJson.put("drawl", "")
        //日志唯一id，用于排重日志， 注意：这里通过uuid生成一个随机数，确保每次日志ID不一样
        livenJson.put("cossack", PhoneInfoUtil.getUUID())
        //操作系统；映射关系：{“juncture”: “android”, “pembroke”: “ios”, “methanol”: “web”}
        livenJson.put("notify", "juncture")
        //当前的包名称，a.b.c
        livenJson.put("footwork", App.appContext.packageName)
        //应用发布的渠道，可不用填写，国内注意渠道包需要有该字段
        livenJson.put("tar", "")
        json.put("liven", livenJson)

        val julepJson = JSONObject()
        //网络类型：wifi，3g等，非必须，和产品确认是否需要分析网络类型相关的信息，此参数可能需要系统权限
        julepJson.put("irma", PhoneInfoUtil.netWorkTypeStr())
        //手机厂商，huawei、oppo
        julepJson.put("tampon", Build.MANUFACTURER)
        //cpu型号
        julepJson.put("neuroses", PhoneInfoUtil.getCpu())
        //用户排重字段
        julepJson.put("lounge", PhoneInfoUtil.getAndroidId())
        //安卓sdk版本号，数字
        julepJson.put("thrall", Build.VERSION.SDK_INT)
        //客户端IP地址，获取的结果需要判断是否为合法的ip地址  todo
        julepJson.put("brunt", "")
        //android App需要有该字段，原值  AndroidId
        julepJson.put("skittle", PhoneInfoUtil.getAndroidId())
        //应用的版本
        julepJson.put("pellet", PhoneInfoUtil.appVersionName())
        //电池电量剩余百分比
        julepJson.put("midpoint", PhoneInfoUtil.getBattery())
        //存储空间，M    需要进行urlencoding
        julepJson.put("cutworm", PhoneInfoUtil.getStorageSize())
        //没有开启google广告服务的设备获取不到，但是必须要尝试获取，用于归因，原值，google广告id
        julepJson.put("canon", SharePreferenceUtil.getString(ConfigurationUtil.GAID))
        //手机型号
        julepJson.put("henpeck", Build.MODEL)
        json.put("julep", julepJson)
        return json
    }

    fun createInstallEventJson(): JSONObject {
        val installJson = JSONObject()
        //系统构建版本，Build.ID， 以 build/ 开头
        installJson.put("delilah", "build/${Build.ID}")
        //google referrer 中的 install_referrer部分
        installJson.put("irate", SharePreferenceUtil.getString(ConfigurationUtil.INSTALL_REFERRER))
        //google referrer 中的 install_version 部分
        installJson.put("louis", SharePreferenceUtil.getString(ConfigurationUtil.INSTALL_VERSION))
        //webView中的user_agent, 注意为webView的，android中的userAgent有;wv关键字
        installJson.put("bahama", WebView(App.appContext).settings.userAgentString)
        //用户是否启用了限制跟踪，0：没有限制，1：限制了；映射关系：{“codicil”: 0, “madman”: 1}
        installJson.put("kelvin", SharePreferenceUtil.getString(ConfigurationUtil.LIMIT_TRACK))
        //引荐来源网址点击事件发生时的客户端时间戳（以秒为单位）
        installJson.put("brushy", SharePreferenceUtil.getLong(ConfigurationUtil.CLICK_TIME_SECOND))
        //应用安装开始时的客户端时间戳（以秒为单位）
        installJson.put(
            "piggish", SharePreferenceUtil.getLong(ConfigurationUtil.INSTALL_BEGIN_TIME_SECOND)
        )
        //引荐来源网址点击事件发生时的服务器端时间戳（以秒为单位）
        installJson.put(
            "wop", SharePreferenceUtil.getLong(ConfigurationUtil.CLICK_TIME_SERVER_SECOND)
        )
        //应用安装开始时的服务器端时间戳（以秒为单位）
        installJson.put(
            "earthman",
            SharePreferenceUtil.getLong(ConfigurationUtil.INSTALL_BEGIN_TIME_SERVER_SECOND)
        )
        //应用首次安装的时间（以秒为单位）
        installJson.put("patrol", PhoneInfoUtil.getAppFirstInstallTime())
        //应用最后一次更新的时间（以秒为单位）
        installJson.put("then", PhoneInfoUtil.getAppLastRefreshTime())
        val commonEventJson = createCommonEventJson()
        commonEventJson.put("intimate", installJson)
        return commonEventJson
    }

    fun createSessionEventJson(): JSONObject {
        val commonEventJson = createCommonEventJson()
        commonEventJson.put("chiefdom", JSONObject())
        return commonEventJson
    }

    private fun createAdviseEventJson(
        commonJson: JSONObject,
        reportEventBean: AdReportEventBean
    ): JSONObject {
        //预估收益,单位为美元乘以十的六次方
        commonJson.put("synge", reportEventBean.estimatedIncome)
        //预估收益的货币单位
        commonJson.put("antigen", reportEventBean.monetaryUnit)
        //广告网络，广告真实的填充平台，例如admob的bidding，填充了Facebook的广告，此值为Facebook
        commonJson.put("whatd", reportEventBean.adPlatform)
        //广告SDK，admob，max等
        commonJson.put("tigress", reportEventBean.adSource)
        //广告位id
        commonJson.put("either", reportEventBean.adId)
        //广告位逻辑编号，例如：page1_bottom, connect_finished
        commonJson.put("europe", reportEventBean.adPosId)
        //真实广告网络返回的广告id，海外获取不到，不传递该字段
        commonJson.put("adolph", reportEventBean.adRitId)
        //广告场景，置空
        commonJson.put("studio", reportEventBean.adSense)
        //广告类型，插屏，原生，banner，激励视频等
        commonJson.put("gunfire", reportEventBean.adType)
        //google ltvpingback的预估收益类型
        commonJson.put("dingo", reportEventBean.precisionType)
        //广告加载时候的ip地址
        commonJson.put("riggs", reportEventBean.adLoadIp)
        //广告显示时候的ip地址
        commonJson.put("hayfield", reportEventBean.adShowIp)
        //广告加载时候的城市
        commonJson.put("sig_biok", reportEventBean.adLoadCity)
        //广告显示时候的城市
        commonJson.put("sig_retim", reportEventBean.adShowCity)
        //广告SDK的版本号
        commonJson.put("duma", reportEventBean.adSdkVersion)
        //广告事件
        commonJson.put("mousy", "randolph")
        return commonJson
    }

    private fun createReportEventBean(
        adValue: AdValue? = null, adWrap: AdWrap, className: String
    ): AdReportEventBean {
        val reportEventBean = AdReportEventBean()
        val valueMicros = adValue?.valueMicros
        val currencyCode = adValue?.currencyCode
        val precisionType = adValue?.precisionType
        if (valueMicros != null) {
            reportEventBean.estimatedIncome = valueMicros
        }
        if (currencyCode != null) {
            reportEventBean.monetaryUnit = currencyCode
        }
        reportEventBean.precisionType = precisionType?.let { it -> getPrecisionType(it) }.toString()
        reportEventBean.adPlatform = getAdPlatform(className)
        reportEventBean.adSource = adWrap.adBean?.ssv_source ?: ""
        reportEventBean.adId = adWrap.adBean?.ssv_id ?: ""
        reportEventBean.adPosId = adWrap.adSpaceName
        reportEventBean.adRitId = ""
        reportEventBean.adSense = ""
        reportEventBean.adType = adWrap.adBean?.ssv_type ?: ""

        reportEventBean.adLoadIp = adWrap.adLoadIp
        reportEventBean.adLoadCity = adWrap.adLoadCity
        adWrap.refreshAdShowIpAndCity(reportEventBean)
        reportEventBean.adSdkVersion = "22.1.0"
        return reportEventBean
    }

    private fun getPrecisionType(type: Int): String {
        when (type) {
            0 -> {
                return "UNKNOWN"
            }
            1 -> {
                return "ESTIMATED"
            }
            2 -> {
                return "PUBLISHER_PROVIDED"
            }
            3 -> {
                return "PRECISE"
            }
        }
        return ""
    }

    private fun getAdPlatform(className: String): String {
        return if (className.contains("Facebook") || className.contains("facebook")) {
            "Facebook"
        } else {
            "adMob"
        }
    }

    fun sendAdvertiseEvent(adWrap: AdWrap?) {
        adWrap?.run {
            when (val ad = ad) {
                is AppOpenAd -> {
                    ad.setOnPaidEventListener {
                        val className = ad.responseInfo.mediationAdapterClassName as String
                        val reportEventBean = createReportEventBean(it, this, className)
                        val reportAdviseEvent =
                            createAdviseEventJson(createCommonEventJson(), reportEventBean)
                        NetworkUtil.reportEvent(NetworkUtil.EventType.OPEN_AD, reportAdviseEvent)
                    }
                }
                is InterstitialAd -> {
                    ad.setOnPaidEventListener {
                        val className = ad.responseInfo.mediationAdapterClassName as String
                        val reportEventBean = createReportEventBean(it, this, className)
                        val reportAdviseEvent =
                            createAdviseEventJson(createCommonEventJson(), reportEventBean)
                        NetworkUtil.reportEvent(NetworkUtil.EventType.INTER_AD, reportAdviseEvent)
                    }
                }

                is NativeAd -> {
                    ad.setOnPaidEventListener {
                        val className = ad.responseInfo?.mediationAdapterClassName as String
                        val reportEventBean = createReportEventBean(it, this, className)
                        val reportAdviseEvent =
                            createAdviseEventJson(createCommonEventJson(), reportEventBean)
                        NetworkUtil.reportEvent(NetworkUtil.EventType.NATIVE_AD, reportAdviseEvent)
                    }
                }
            }
        }

    }
}