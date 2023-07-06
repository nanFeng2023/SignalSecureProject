package com.ssv.signalsecurevpn.util

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Base64
import com.google.firebase.ktx.BuildConfig
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.activity.App
import com.ssv.signalsecurevpn.bean.AdBean
import com.ssv.signalsecurevpn.bean.AdDataResult
import com.ssv.signalsecurevpn.bean.OptionResult
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.call.BusinessProcessCallBack
import com.ssv.signalsecurevpn.call.IpDelayTestCallBack
import com.ssv.signalsecurevpn.json.PhoneInfoUtil
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object NetworkUtil {
    private const val BASE_URL = "https://ipinfo.io/json"
    private const val COUNTRY_HK = "HK"//HongKong 香港
    private const val COUNTRY_CN = "CN"//China 大陆
    private const val COUNTRY_IRAN = "Iran"//伊朗
    private const val COUNTRY_MACAU = "Macau" //澳门
    private var countryCode = ""

    //smart
    lateinit var cityList: ArrayList<String>
    lateinit var serviceDataList: ArrayList<VpnBean>

    var adDataResult: AdDataResult? = null
    var isRestrictArea = false//是否是限制地区
    lateinit var optionResult: OptionResult
    var isLoadingServerData = false

    private fun obtainNativeJsonData(jsonDataName: String): StringBuilder {
        val assetManager = App.appContext.assets
        val inputStreamReader = InputStreamReader(assetManager.open(jsonDataName), "UTF-8")
        val bufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder = StringBuilder()
        val iterator = bufferedReader.lineSequence().iterator()
        while (iterator.hasNext()) {
            val line = iterator.next()
            stringBuilder.append(line)
        }
        bufferedReader.close()
        inputStreamReader.close()
        return stringBuilder
    }

    /**
     * qcfsJCk -->mode
     * dyzyzN-->ip
     * nbfAxvDp-->port
     * ZVUqe-->user_name
     * yjIYnO-->user_pwd
     * lDLldJv-->encrypt
     * xgXa-->city
     * ZDadahPnG-->country_name
     * SEJ-->country_code
     *
     * AQp-->smart_list
     * RLx-->server_list
     */
    private fun parseNewServiceData(data: String, dataList: ArrayList<VpnBean>) {
        val jsonObject = JSONObject(data)
        val serverList = jsonObject.optJSONArray("RLx")
        for (i in 0 until (serverList?.length()!!)) {
            val server = serverList.optJSONObject(i)
            val account = server.optString("lDLldJv")
            val port = server.optInt("nbfAxvDp")
            val pwd = server.optString("yjIYnO")
            val country = server.optString("ZDadahPnG")
            val city = server.optString("xgXa")
            val ip = server.optString("dyzyzN")
            val vpnBean = VpnBean()
            vpnBean.pwd = pwd
            vpnBean.account = account
            vpnBean.port = port
            vpnBean.country = country
            vpnBean.city = city
            vpnBean.ip = ip
            dataList.add(vpnBean)
        }
    }

    private fun parseServiceData(data: String, dataList: ArrayList<VpnBean>) {
        val jsonObject = JSONObject(data)
        val optJSONArray = jsonObject.optJSONArray("sigvn_ser")
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("NetworkUtil----parseServiceData()---本地数据:$optJSONArray")
        for (i in 0 until (optJSONArray?.length()!!)) {
            val obj = optJSONArray.optJSONObject(i)
            val account = obj.optString("sigva")
            val port = obj.optInt("sigvn")
            val pwd = obj.optString("sigvnord")
            val country = obj.optString("sigvnry")
            val city = obj.optString("sigvniy")
            val ip = obj.optString("sigvnip")
            val vpnBean = VpnBean()
            vpnBean.pwd = pwd
            vpnBean.account = account
            vpnBean.port = port
            vpnBean.country = country
            vpnBean.city = city
            vpnBean.ip = ip
            dataList.add(vpnBean)
        }
    }

    private fun parseNewCityListData(data: String, cityList: ArrayList<String>) {
        val jsonObject = JSONObject(data)
        val smartList = jsonObject.optJSONArray("AQp")
        for (i in 0 until (smartList?.length()!!)) {
            val city = smartList.optJSONObject(i).optString("xgXa")
            cityList.add(city.toString())
        }
    }

    private fun parseCityListData(data: String, cityList: ArrayList<String>) {
        val jsonObject = JSONObject(data)
        val optJSONArray = jsonObject.optJSONArray("sigvn_smar")
        for (i in 0 until (optJSONArray?.length()!!)) {
            val city = optJSONArray.get(i)
            cityList.add(city.toString())
        }
    }

    /*
     * 获取当前的网络状态 ：没有网络-0：WIFI网络1：4G网络-4：3G网络-3：2G网络-2
    */
    fun getNetworkType(context: Context): Int {
        var netType = 0
        val manager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isAvailable) {
            return netType
        }
        val nType = networkInfo.type
        if (nType == ConnectivityManager.TYPE_WIFI) {
            //WIFI
            netType = 1
        } else if (nType == ConnectivityManager.TYPE_MOBILE) {
            val nSubType = networkInfo.subtype
            val telephonyManager: TelephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            netType =
                if (nSubType == TelephonyManager.NETWORK_TYPE_LTE && !telephonyManager.isNetworkRoaming) {
                    //4G
                    4
                } else if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS || nSubType == TelephonyManager.NETWORK_TYPE_HSDPA || (nSubType == TelephonyManager.NETWORK_TYPE_EVDO_0 && !telephonyManager.isNetworkRoaming)) {
                    //3G   联通的3G为UMTS或HSDPA 电信的3G为EVDO
                    3
                } else if (nSubType == TelephonyManager.NETWORK_TYPE_GPRS || nSubType == TelephonyManager.NETWORK_TYPE_EDGE || (nSubType == TelephonyManager.NETWORK_TYPE_CDMA && !telephonyManager.isNetworkRoaming)) {
                    //2G 移动和联通的2G为GPRS或EGDE，电信的2G为CDMA
                    2
                } else {
                    2
                }
        }
        return netType
    }

    suspend fun delayTest(vpnBean: VpnBean, callBack: IpDelayTestCallBack?, timeout: Int = 1) {
        var delay = -1
        val count = 1//重试次数
        val ip = vpnBean.ip
        val cmd = "/system/bin/ping -c $count -w $timeout $ip"
        withContext(Dispatchers.IO) {
            val r = ping(cmd)
            if (r != null) {
                try {
                    val index: Int = r.indexOf("min/avg/max/mdev")
                    if (index != -1) {
                        val tempInfo: String = r.substring(index + 19)
                        val temps = tempInfo.split("/".toRegex()).toTypedArray()
                        delay = temps[0].toFloat().roundToInt()//min
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                callBack?.onIpDelayTest(vpnBean, delay)
            }
        }
    }

    /*ping命令*/
    private fun ping(cmd: String): String? {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(cmd) //执行ping指令
            val inputStream = process!!.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (null != reader.readLine().also { line = it }) {
                sb.append(line)
                sb.append("\n")
            }
            reader.close()
            inputStream.close()
            return sb.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            process?.destroy()
        }
        return null
    }

    fun obtainServiceData(data: String?) {
        cityList = ArrayList()
        serviceDataList = ArrayList()
        //服务器列表第一个smart服务器
        val firstItemVpnBean = VpnBean()
        firstItemVpnBean.country = ProjectUtil.DEFAULT_FAST_SERVERS
        serviceDataList.add(firstItemVpnBean)
        if (!data.isNullOrEmpty()) {
            parseNewServiceData(data, serviceDataList)
            parseNewCityListData(data, cityList)
        }
    }

    fun obtainAdData() {
        val adData: String? = SharePreferenceUtil.getString(AdMob.SIGVN_AD)
        Timber.tag(ConfigurationUtil.LOG_TAG).d("NetworkUtil----obtainAdData()---远端广告数据:$adData")
        adDataResult = if (adData != null && !TextUtils.isEmpty(adData)) {
            parseAdData(adData)
        } else {
            //2.没有1才本地
            val obtainNativeJsonData = obtainNativeJsonData("ad.json")
            parseAdData(obtainNativeJsonData.toString())
        }
    }

    fun obtainPlanData() {
        val planStr = SharePreferenceUtil.getString(ConfigurationUtil.PLAN_KEY)
        optionResult = if (!planStr.isNullOrEmpty()) {
            parsePlanData(planStr)
        } else {
            parsePlanData(obtainNativeJsonData("plan.json").toString())
        }
    }

    private fun parsePlanData(data: String): OptionResult {
        val json = JSONObject(data)
        val optionResult = OptionResult()
        optionResult.sig_home = json.optString(ConfigurationUtil.PLAN_PARAM_KEY1)
        optionResult.sig_yes = json.optString(ConfigurationUtil.PLAN_PARAM_KEY2)
        optionResult.sig_tio = json.optString(ConfigurationUtil.PLAN_PARAM_KEY3)
        optionResult.a_cloak = json.optString(ConfigurationUtil.PLAN_PARAM_KEY4)
        return optionResult
    }

    private fun parseAdData(data: String): AdDataResult {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("NetworkUtil----parseAdData()---data:$data")
        val jsonObject = JSONObject(data)
        val adDataResult = AdDataResult()
        val ssv_show_upper_limit = jsonObject.optInt("sigvns")
        adDataResult.ssv_show_upper_limit = ssv_show_upper_limit
        val ssv_click_upper_limit = jsonObject.optInt("sigvnc")
        adDataResult.ssv_click_upper_limit = ssv_click_upper_limit
        val ad_open_on =
            parseAdData(jsonObject.optJSONArray(ConfigurationUtil.AD_SPACE_OPEN) as JSONArray)
        adDataResult.ssv_ad_open_on = ad_open_on
        val ad_native_home =
            jsonObject.optJSONArray(ConfigurationUtil.AD_SPACE_NATIVE_HOME)?.let { parseAdData(it) }
        adDataResult.ssv_ad_native_home = ad_native_home
        val ad_native_result = jsonObject.optJSONArray(ConfigurationUtil.AD_SPACE_NATIVE_RESULT)
            ?.let { parseAdData(it) }
        adDataResult.ssv_ad_native_result = ad_native_result
        val ad_inter_click = jsonObject.optJSONArray(ConfigurationUtil.AD_SPACE_INTER_CONNECTION)
            ?.let { parseAdData(it) }
        adDataResult.ssv_ad_inter_click = ad_inter_click
        val ad_inter_ib =
            jsonObject.optJSONArray(ConfigurationUtil.AD_SPACE_INTER_BACK)?.let { parseAdData(it) }
        adDataResult.ssv_ad_inter_ib = ad_inter_ib
        return adDataResult
    }

    private fun parseAdData(optJSONArray: JSONArray): ArrayList<AdBean> {
        val adBeanList: ArrayList<AdBean> = ArrayList()
        for (i in 0 until (optJSONArray.length())) {
            val obj = optJSONArray.optJSONObject(i)
            val sigvn_id = obj.optString("sigvn_id")
            val sigvn_s = obj.optString("sigvn_s")
            val sigvn_t = obj.optString("sigvn_t")
            val sigvn_p = obj.optInt("sigvn_p")
            val adBean = AdBean()
            adBean.ssv_id = sigvn_id
            adBean.ssv_source = sigvn_s
            adBean.ssv_type = sigvn_t
            adBean.ssv_priority = sigvn_p
            adBeanList.add(adBean)
        }
        return adBeanList
    }

    /*检测IP为主，地区限制为辅*/
    fun detectionIp(businessProcessCallBack: BusinessProcessCallBack?) {
        isRestrictArea = false
        OkGo.get<String>(BASE_URL).tag(this).cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("NetworkUtil----detectionIp()---onSuccess()")
                    response?.let {
                        try {
                            val jsonObj = JSONObject(it.body())
                            val country = jsonObj.optString("country").toString()
                            val ip = jsonObj.optString("ip")
                            SharePreferenceUtil.putString(ConfigurationUtil.PUBLIC_NETWORK_IP, ip)
                            SharePreferenceUtil.putString(ConfigurationUtil.CUR_CONNECT_IP, ip)
                            restrictArea(country, businessProcessCallBack)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("NetworkUtil----detectionIp()---onError()---error:$response")
                    val country = Locale.getDefault().country
                    restrictArea(country, businessProcessCallBack)
                }
            })
    }

    private fun restrictArea(country: String, businessProcessCallBack: BusinessProcessCallBack?) {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("NetworkUtil----restrictArea()---当前国家:$country")
        countryCode = country
        isRestrictArea =
            (COUNTRY_HK == country/*|| COUNTRY_CN == country*/ || COUNTRY_MACAU == country || COUNTRY_IRAN == country)
        if (isRestrictArea) {
            businessProcessCallBack?.onBusinessProcess(true)
        } else {
            businessProcessCallBack?.onBusinessProcess(false)
        }
    }

    fun reportEvent(type: EventType, json: JSONObject) {
        Timber.d("reportEvent()---event type:$type---params:$json")
        OkGo.post<String>(ConfigurationUtil.TBA_SERVER_URL)
            .upJson(json)
            .headers("drawl", "")
            .headers("footwork", App.appContext.packageName)
            .params("drawl", "")
            .params("footwork", URLEncoder.encode(App.appContext.packageName, "UTF-8"))
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Timber.d("reportEvent()---onSuccess()---event type:$type---response:${response?.isSuccessful}")
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Timber.d("reportEvent()---onError()---event type:$type---response:${response?.exception.toString()}")
                    eventUploadFail(type, json)
                }
            })
    }

    private fun eventUploadFail(type: EventType, json: JSONObject) {
        when (type) {
            EventType.INSTALL -> {
                SharePreferenceUtil.putString(ConfigurationUtil.FAIL_EVENT_INSTALL, json.toString())
            }
            EventType.SESSION -> {
                SharePreferenceUtil.putString(ConfigurationUtil.FAIL_EVENT_SESSION, json.toString())
            }
            else -> {}
        }
    }

    enum class EventType {
        INSTALL,
        SESSION,
        OPEN_AD,
        INTER_AD,
        NATIVE_AD,
        DOT_EVENT
    }

    fun reqClock(retry: Int = 2) {
        if (BuildConfig.DEBUG)
            return
        var cloakReqRetry = retry
        val url = buildString {
            append(ConfigurationUtil.CLOAK_URL)
            append(genParamsStr(buildParam()))
        }
        OkGo.get<String>(url)
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Timber.d("reqClock()---onSuccess()---response:${response?.isSuccessful}")
                    val body = response?.body()
                    body?.run {
//                        if (this == "toefl") {
//                            UserReferrerUtils.planBean.cloakType = PlanBean.CloakTYPE.BLACK_LIST
//                        } else if (this == "indorse") {
//                            UserReferrerUtils.planBean.cloakType = PlanBean.CloakTYPE.NORMAL
//                        }
                    }
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Timber.d("reqClock()---onSuccess()---response:${response?.exception.toString()}")
                    GlobalScope.launch(Dispatchers.IO) {
                        if (retry > 0) {
                            delay(10000)
                            cloakReqRetry--
                            reqClock(cloakReqRetry)
                        }
                    }
                }
            })
    }

    private fun buildParam(): HashMap<String, String> {
        val paramsMap = HashMap<String, String>()
        paramsMap["lounge"] = PhoneInfoUtil.getAndroidId()
        //客服端发生时间
        paramsMap["bone"] = System.currentTimeMillis().toString()
        paramsMap["henpeck"] = Build.MODEL
        paramsMap["footwork"] = App.appContext.packageName
        paramsMap["evelyn"] = Build.VERSION.RELEASE
        paramsMap["drawl"] = ""
        paramsMap["canon"] = SharePreferenceUtil.getString(ConfigurationUtil.GAID).toString()
        paramsMap["skittle"] = PhoneInfoUtil.getAndroidId()
        paramsMap["notify"] = "juncture"
        paramsMap["reach"] = ""
        paramsMap["pellet"] = PhoneInfoUtil.appVersionName()
        paramsMap["winslow"] = PhoneInfoUtil.getCarrierName()
        return paramsMap
    }

    private fun genParamsStr(params: Map<String, String>): String {
        if (params.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        builder.append("?")
        val keys = params.keys
        val iterKey = keys.iterator()
        val firstKey = iterKey.next()
        builder.append(firstKey + "=" + params[firstKey])
        while (iterKey.hasNext()) {
            val key = iterKey.next()
            val value = params[key]
            var valueEncoded = ""
            try {
                valueEncoded = URLEncoder.encode(value, "UTF-8")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            builder.append("&$key=$valueEncoded")
        }
        return builder.toString()
    }

    fun requestServerData() {
        isLoadingServerData = true
        var countryCode = "ZZ"
        if (countryCode.isNotEmpty()) {
            countryCode = this.countryCode
        }
        Timber.d("requestServerData()---countryCode:$countryCode")
        FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_REQ_SERVER_DATA)
        Timber.d("---upLoadLogEvent:${ConfigurationUtil.DOT_REQ_SERVER_DATA}")
        val startReqRemoteTime = System.currentTimeMillis()
        OkGo.get<String>(ConfigurationUtil.REQ_REMOTE_SERVER_URL)
            .headers("SEJ", countryCode)
            .headers("BKO", App.appContext.packageName)
            .headers("ULVQO", PhoneInfoUtil.getAndroidId())
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Timber.d("requestServerData()---onSuccess()---response:${response?.body()}")
                    val body = response?.body()
                    body?.run {
                        try {
                            val decodeResponse = decodeData(this)
                            Timber.d("requestServerData()---decodeResponse:${decodeResponse}")
                            val jsonObject = JSONObject(decodeResponse)
                            val code = jsonObject.optInt("code")
                            val msg = URLDecoder.decode(jsonObject.optString("msg"), "UTF-8")
                            val data = jsonObject.optJSONObject("data")
                            Timber.d("requestServerData()---code:$code---msg:$msg---data:$data")
                            when (code) {
                                200 -> {
                                    //打点事件统计
                                    uploadEvent(startReqRemoteTime)

                                    if (data != null) {
                                        obtainServiceData(data.toString())
                                        SharePreferenceUtil.putString(
                                            ConfigurationUtil.REMOTE_SERVER_DATA,
                                            data.toString()
                                        )
                                    }
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    isLoadingServerData = false
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Timber.d("requestServerData()---onError()---error:${response?.exception.toString()}")
                    isLoadingServerData = false
                }
            })
    }

    private fun decodeData(data: String): String {
        val resultStr = data.substring(41, data.length)
        val reverseFirstStr = StringBuffer(resultStr).reverse()
        return base64Decode(reverseFirstStr.toString())
    }

    private fun base64Decode(str: String): String {
        val responseByte = Base64.decode(str, Base64.DEFAULT)
        return String(responseByte)
    }

    private fun uploadEvent(startReqRemoteTime: Long) {
        FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_GET_SERVER_DATA)
        val obtainReqRemoteTime = System.currentTimeMillis()
        val obtainTime = ((obtainReqRemoteTime - startReqRemoteTime) / 1000).toInt()
        val bundle = Bundle()
        bundle.putLong("time", obtainTime.toLong())
        FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_REQ_SERVER_DATA_TOTAL_TIME, bundle)
    }

}
