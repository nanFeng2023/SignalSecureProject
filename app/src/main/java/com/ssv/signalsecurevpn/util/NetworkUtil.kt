package com.ssv.signalsecurevpn.util

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.App
import com.ssv.signalsecurevpn.bean.AdBean
import com.ssv.signalsecurevpn.bean.AdDataResult
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.call.BusinessProcessCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object NetworkUtil {
    private const val BASE_URL = "https://ipinfo.io/json"
    private const val COUNTRY_HK = "HK"//HongKong 香港
    private const val COUNTRY_CN = "CN"//China 大陆 //todo 正是包时候记得打开
    private const val COUNTRY_IRAN = "Iran"//伊朗
    private const val COUNTRY_MACAU = "Macau" //澳门

    //smart
    lateinit var cityList: ArrayList<String>
    lateinit var serviceDataList: ArrayList<VpnBean>

    var adDataResult: AdDataResult? = null
    var isRestrictArea = false//是否是限制地区

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

    private fun parseServiceData(data: String, dataList: ArrayList<VpnBean>) {
        val jsonObject = JSONObject(data)
        val optJSONArray = jsonObject.optJSONArray("list")
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("NetworkUtil----parseServiceData()---本地数据:$optJSONArray")
        for (i in 0 until (optJSONArray?.length()!!)) {
            val obj = optJSONArray.optJSONObject(i)
            val pwd = obj.optString("ssv_pd")
            val account = obj.optString("ssv_act")
            val port = obj.optInt("ssv_pt")
            val country = obj.optString("ssv_coy")
            val city = obj.optString("ssv_ciy")
            val ip = obj.optString("ssv_ip")
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

    private fun parseCityListData(data: String, cityList: ArrayList<String>) {
        val jsonObject = JSONObject(data)
        val optJSONArray = jsonObject.optJSONArray("cityList")
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
                } else if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS || nSubType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || (nSubType == TelephonyManager.NETWORK_TYPE_EVDO_0
                            && !telephonyManager.isNetworkRoaming)
                ) {
                    //3G   联通的3G为UMTS或HSDPA 电信的3G为EVDO
                    3
                } else if (nSubType == TelephonyManager.NETWORK_TYPE_GPRS
                    || nSubType == TelephonyManager.NETWORK_TYPE_EDGE
                    || (nSubType == TelephonyManager.NETWORK_TYPE_CDMA
                            && !telephonyManager.isNetworkRoaming)
                ) {
                    //2G 移动和联通的2G为GPRS或EGDE，电信的2G为CDMA
                    2
                } else {
                    2
                }
        }
        return netType
    }

    suspend fun delayTest(ip: String, timeout: Int = 1): Int {
        var delay = Int.MAX_VALUE
        val count = 1
        val cmd = "/system/bin/ping -c $count -w $timeout $ip"
        return withContext(Dispatchers.IO) {
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
            }
            delay
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

    fun obtainServiceData() {
        cityList = ArrayList()
        serviceDataList = ArrayList()
        //服务器列表第一个smart服务器
        val firstItemVpnBean = VpnBean()
        firstItemVpnBean.country = ProjectUtil.DEFAULT_FAST_SERVERS
        serviceDataList.add(firstItemVpnBean)
        //1.先服务器接口获取列表
        val serviceVpnData: String? = SharePreferenceUtil.getString(AdMob.SIGVN_SERVICE)
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("NetworkUtil----obtainServiceData()---远端服务器vpn数据:$serviceVpnData")
        if (serviceVpnData != null && !TextUtils.isEmpty(serviceVpnData)) {
            parseServiceData(serviceVpnData, serviceDataList)
            parseCityListData(serviceVpnData, cityList)
        } else {
            //2.没有1才走本地
            val obtainNativeJsonData = obtainNativeJsonData("data.json")
            parseServiceData(obtainNativeJsonData.toString(), serviceDataList)
            parseCityListData(obtainNativeJsonData.toString(), cityList)
        }
    }

    fun obtainAdData() {
        val adData: String? = SharePreferenceUtil.getString(AdMob.SIGVN_AD)
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("NetworkUtil----obtainAdData()---远端广告数据:$adData")
        adDataResult = if (!TextUtils.isEmpty(adData)) {
            adData?.let { parseAdData(it) }
        } else {
            //2.没有1才本地
            val obtainNativeJsonData = obtainNativeJsonData("ad.json")
            parseAdData(obtainNativeJsonData.toString())
        }
    }

    private fun parseAdData(data: String): AdDataResult {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("NetworkUtil----parseAdData()---data:$data")
        val jsonObject = JSONObject(data)
        val adDataResult = AdDataResult()
        val ssv_show_upper_limit = jsonObject.optInt("sigvns")
        adDataResult.ssv_show_upper_limit = ssv_show_upper_limit
        val ssv_click_upper_limit = jsonObject.optInt("sigvnc")
        adDataResult.ssv_click_upper_limit = ssv_click_upper_limit
        val ad_open_on = parseAdData(jsonObject.optJSONArray("sigvn_on") as JSONArray)
        adDataResult.ssv_ad_open_on = ad_open_on
        val ad_native_home = jsonObject.optJSONArray("sigvn_nhome")?.let { parseAdData(it) }
        adDataResult.ssv_ad_native_home = ad_native_home
        val ad_native_result = jsonObject.optJSONArray("sigvn_nresult")?.let { parseAdData(it) }
        adDataResult.ssv_ad_native_result = ad_native_result
        val ad_inter_click = jsonObject.optJSONArray("sigvn_click")?.let { parseAdData(it) }
        adDataResult.ssv_ad_inter_click = ad_inter_click
        val ad_inter_ib = jsonObject.optJSONArray("sigvn_ib")?.let { parseAdData(it) }
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
        OkGo.get<String>(BASE_URL)
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("NetworkUtil----detectionIp()---onSuccess()")
                    response?.let {
                        try {
                            val jsonObj = JSONObject(it.body())
                            val country = jsonObj.opt("country")?.toString()
                            country?.let { it1 -> restrictArea(it1, businessProcessCallBack) }
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
        isRestrictArea =
            (COUNTRY_HK == country/*|| COUNTRY_CN == country*/ || COUNTRY_MACAU == country || COUNTRY_IRAN == country)
        if (isRestrictArea) {
            businessProcessCallBack?.onBusinessProcess(true)
        } else {
            businessProcessCallBack?.onBusinessProcess(false)
        }
    }
}
