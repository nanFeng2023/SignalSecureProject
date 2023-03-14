package com.ssv.signalsecurevpn

import android.widget.ImageView
import android.widget.TextView
import com.secure.fast.signalvpn.R
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
    }

    override fun bindViewId() {
        ivBack = findViewById(R.id.iv_back)
        ivBack.setOnClickListener { finish() }

        tvTitle = findViewById(R.id.tv_title)

        tvConnectTime = findViewById(R.id.tv_connect_time_result)
        tvConnectState = findViewById(R.id.tv_connect_state_result)
        ivCountry = findViewById(R.id.iv_country_result)
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