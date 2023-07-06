package com.ssv.signalsecurevpn.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.adapter.SettingAdapter
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.ad.AdShowStateCallBack
import com.ssv.signalsecurevpn.util.*
import com.ssv.signalsecurevpn.widget.AlertDialogUtil

/*
* VPN选择页面
* */
class VpnSelectActivity : BaseActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var ivBack: ImageView
    private lateinit var tvTitle: TextView
    private var settingAdapter: SettingAdapter? = null

    override fun businessProcess() {
        settingAdapter = SettingAdapter(NetworkUtil.serviceDataList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = settingAdapter
        settingAdapter?.onItemViewClick = { _, position ->
            //展示系统弹窗
            showAlertDialog(position)
        }
        FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_VPN_SERVER_SHOW)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PlanUtil.isShowInterAd()) {
                    AdManager.showAd(
                        this@VpnSelectActivity,
                        AdMob.AD_INTER_IB,
                        object : AdShowStateCallBack {
                            override fun onAdDismiss() {
                                finishPageAndLoadAd()
                            }

                            override fun onAdShowed() {

                            }

                            override fun onAdShowFail() {
                                finishPageAndLoadAd()
                            }

                            override fun onAdClicked() {

                            }
                        })
                } else {
                    finish()
                }
            }
        })
    }

    override fun bindViewId() {
        rv = findViewById(R.id.rv_setting)
        ivBack = findViewById(R.id.iv_back)
        tvTitle = findViewById(R.id.tv_title)
        tvTitle.setText(R.string.setting_location)

        ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        if (PlanUtil.isShowInterAd()) {
            //请求插屏广告
            AdManager.loadAd(AdMob.AD_INTER_IB, null)
        }
    }

    override fun getLayout(): Int {
        return R.layout.activity_vpn_select
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setResultForActivity(position: Int) {
        //上次选中的城市
        val lastSelectCity = SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
        SharePreferenceUtil.putString(ProjectUtil.LAST_SELECT_CITY, lastSelectCity)
        //保存选中的VPN 格式：国家-城市
        SharePreferenceUtil.putString(
            ProjectUtil.CUR_SELECT_CITY,
            NetworkUtil.serviceDataList[position].getName()
        )

        //页面回传值
        val intent = Intent()
        val bundle = Bundle()
        bundle.putInt(ProjectUtil.CUR_SELECT_SERVICE_POSITION, position)
        intent.putExtras(bundle)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showAlertDialog(position: Int) {
        if (ProjectUtil.connected) {
            AlertDialogUtil().createDialog(
                this@VpnSelectActivity, null,
                "whether to disconnect the current connection", "yes",
                { _, _ ->
                    setResultForActivity(position)
                }, "no", null
            )
        } else {
            setResultForActivity(position)
        }
    }

    private fun finishPageAndLoadAd() {
        finish()
        overridePendingTransition(0, 0)
    }

}