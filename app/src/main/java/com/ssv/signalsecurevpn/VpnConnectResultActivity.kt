package com.ssv.signalsecurevpn

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.ad.AdShowStateCallBack
import com.ssv.signalsecurevpn.call.TimeDataCallBack
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.ProjectUtil
import com.ssv.signalsecurevpn.util.TimeUtil
import timber.log.Timber
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
    private lateinit var adViewGroup: CardView
    private lateinit var ivAdBg: ImageView
    override fun businessProcess() {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("VpnConnectResultActivity----businessProcess()")
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
        showAdAndLoadAd()
    }

    private fun showAdAndLoadAd() {
        if (AdManager.isAdAvailable(AdMob.AD_NATIVE_RESULT) == true) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("VpnConnectResultActivity----showAdAndLoadAd()---有原生广告缓存")
            ivAdBg.visibility = View.INVISIBLE
            adViewGroup.visibility = View.VISIBLE
            //展示原生广告
            showNativeAd()
        } else {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("VpnConnectResultActivity----showAdAndLoadAd()---没有原生广告缓存")
            ivAdBg.visibility = View.VISIBLE
            adViewGroup.visibility = View.INVISIBLE
            loadNativeAd()
        }
    }

    private fun loadNativeAd() {
        //请求新广告
        AdManager.loadAd(AdMob.AD_NATIVE_RESULT, null)
    }

    private fun showNativeAd() {
        AdManager.showAd(
            this@VpnConnectResultActivity,
            AdMob.AD_NATIVE_RESULT, object : AdShowStateCallBack {
                override fun onAdDismiss() {

                }

                override fun onAdShowed() {
                    loadNativeAd()
                }

                override fun onAdShowFail() {

                }

                override fun onAdClicked() {

                }

            },
            R.layout.layout_native_ad_connect_result,
            adViewGroup
        )
    }

    override fun onResume() {
        super.onResume()
        //引导页过来才刷新
        if (AdMob.isRefreshNativeAd) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("VpnConnectResultActivity----onResume()---刷新原生广告")
            showAdAndLoadAd()
        }
    }

    override fun bindViewId() {
        ivBack = findViewById(R.id.iv_back)
        ivBack.setOnClickListener { finish() }

        tvTitle = findViewById(R.id.tv_title)

        tvConnectTime = findViewById(R.id.tv_connect_time_result)
        tvConnectState = findViewById(R.id.tv_connect_state_result)
        ivCountry = findViewById(R.id.iv_country_result)
        adViewGroup = findViewById(R.id.cd_ad_view_group_connect_result)
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