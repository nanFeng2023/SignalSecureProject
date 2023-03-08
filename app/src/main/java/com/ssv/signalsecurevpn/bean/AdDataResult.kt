package com.ssv.signalsecurevpn.bean

class AdDataResult {
    var ssv_show_upper_limit: Int = 0 //展示上限
    var ssv_click_upper_limit: Int = 0//点击上限
    var ssv_ad_open_on: ArrayList<AdBean>? = null //开屏
    var ssv_ad_native_home: ArrayList<AdBean>? = null //原生1
    var ssv_ad_native_result: ArrayList<AdBean>? = null //原生2
    var ssv_ad_inter_click: ArrayList<AdBean>? = null//插屏1
    var ssv_ad_inter_ib: ArrayList<AdBean>? = null//插屏2

}