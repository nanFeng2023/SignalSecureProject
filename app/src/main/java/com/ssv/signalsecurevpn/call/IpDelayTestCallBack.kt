package com.ssv.signalsecurevpn.call

import com.ssv.signalsecurevpn.bean.VpnBean

interface IpDelayTestCallBack {
    fun onIpDelayTest(vpnBean: VpnBean,ipDelayTime: Int)
}