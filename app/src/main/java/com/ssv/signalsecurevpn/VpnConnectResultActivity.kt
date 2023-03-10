package com.ssv.signalsecurevpn

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.call.TimeDataCallBack
import com.ssv.signalsecurevpn.util.ProjectUtil
import com.ssv.signalsecurevpn.util.TimeUtil
import java.util.*

/*
* vpn连接结果页
* */
class VpnConnectResultActivity : BaseActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvConnectTime: TextView
    private lateinit var tvConnectState: TextView
    private lateinit var timeDataCallBack: TimeDataCallBack
    private lateinit var ivCountry: ImageView
    private lateinit var flAdViewGroup: FrameLayout
    private lateinit var ivAdBg: ImageView
    override fun businessProcess() {
        tvTitle.setText(R.string.result)
        val country = intent.getStringExtra(ProjectUtil.COUNTRY_KEY)
        country?.let { ivCountry.setImageResource(ProjectUtil.selectCountryIcon(it.lowercase(Locale.getDefault()))) }

        timeDataCallBack = object : TimeDataCallBack {
            override fun onTime(time: String) {
                tvConnectTime.text = time
            }

            override fun onResetTime() {

            }
        }
        TimeUtil.dataCallList.add(timeDataCallBack)
        tvConnectTime.text = TimeUtil.curConnectTime
        if (ProjectUtil.connected) {
            tvConnectTime.setTextColor(getColor(R.color.connect_success_color_result))
            tvConnectState.setTextColor(getColor(R.color.connect_success_color_result))
            tvConnectState.text = getString(R.string.connection_succeed)
        } else {
            tvConnectTime.setTextColor(getColor(R.color.connect_fail_color_result))
            tvConnectState.setTextColor(getColor(R.color.connect_fail_color_result))
            tvConnectState.text = getString(R.string.connection_fail)
        }
        if (AdManager.isAdAvailable(AdMob.AD_NATIVE_RESULT) == true) {
            ivAdBg.visibility = View.INVISIBLE
            //展示原生广告
            AdManager.showAd(
                this@VpnConnectResultActivity,
                AdMob.AD_NATIVE_RESULT, null,
                R.layout.layout_native_ad_connect_result,
                flAdViewGroup
            )
        } else {
            ivAdBg.visibility = View.VISIBLE
        }
        //请求新广告
        AdManager.loadAd(AdMob.AD_NATIVE_RESULT, null)
    }

    override fun bindViewId() {
        ivBack = findViewById(R.id.iv_back)
        ivBack.setOnClickListener { finish() }

        tvTitle = findViewById(R.id.tv_title)

        tvConnectTime = findViewById(R.id.tv_connect_time_result)
        tvConnectState = findViewById(R.id.tv_connect_state_result)
        ivCountry = findViewById(R.id.iv_country_result)
        flAdViewGroup = findViewById(R.id.fl_ad_view_group_connect_result)
        ivAdBg = findViewById(R.id.iv_ad_bg_connect_result)
    }

    override fun getLayout(): Int {
        return R.layout.activity_vpn_connect_result
    }

    override fun onDestroy() {
        super.onDestroy()
        TimeUtil.dataCallList.remove(timeDataCallBack)
        if (ProjectUtil.stopped) {//如果连接关闭
            TimeUtil.resetTime()
            //重置主页时间
            TimeUtil.dataCallList[0].onResetTime()
        }
    }
}