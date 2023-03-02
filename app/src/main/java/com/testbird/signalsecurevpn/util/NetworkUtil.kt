package com.testbird.signalsecurevpn.util

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import android.util.Log
import com.testbird.signalsecurevpn.App
import com.testbird.signalsecurevpn.bean.VpnBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object NetworkUtil {
    const val BASE_URL = "https://ipinfo.io/json"
    const val COUNTRY_HK = "HK"//HongKong 香港
    const val COUNTRY_CN = "CN"//China 大陆
    const val COUNTRY_IRAN = "Iran"//伊朗
    const val COUNTRY_MACAU = "Macau" //澳门
    const val PRIVACY_POLICY_URL = "https://www.baidu.com"

    fun obtainNativeJsonData(): StringBuilder {
        val assetManager = App.appContext.assets
        val inputStreamReader = InputStreamReader(assetManager.open("data.json"), "UTF-8")
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

    fun parseData(data: String, dataList: ArrayList<VpnBean>) {
        val jsonObject = JSONObject(data)
        val optJSONArray = jsonObject.optJSONArray("list")
        Log.i("NetworkUtil--parseData", "data:$optJSONArray")
        for (i in 0 until (optJSONArray?.length()!!)) {
            val obj = optJSONArray.optJSONObject(i)
            val pwd = obj.optString("pwd")
            val account = obj.optString("account")
            val port = obj.optInt("port")
            val country = obj.optString("country")
            val city = obj.optString("city")
            val ip = obj.optString("ip")
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

    fun parseCityListData(data: String, cityList: ArrayList<String>) {
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

    /**
     * 获得本机ip地址
     */
    fun GetHostIp(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val ipAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                while (ipAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = ipAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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

    //smart
    lateinit var cityList: ArrayList<String>
    lateinit var serviceDataList: ArrayList<VpnBean>

    fun obtainServiceData() {
        cityList = ArrayList()
        serviceDataList = ArrayList()
        //服务器列表第一个smart服务器
        val firstItemVpnBean = VpnBean()
        firstItemVpnBean.country = ProjectUtil.DEFAULT_FAST_SERVERS
        serviceDataList.add(firstItemVpnBean)
        //1.先服务器接口获取列表

        //2.没有1才走本地
        val obtainNativeJsonData = obtainNativeJsonData()
        parseData(obtainNativeJsonData.toString(), serviceDataList)
        parseCityListData(obtainNativeJsonData.toString(), cityList)
    }

}
