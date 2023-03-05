package com.ssv.signalsecurevpn.call

/*前后台监听回调*/
interface FrontAndBackgroundCallBack {
    fun onAppToFront()
    fun onAppToBackGround()
}