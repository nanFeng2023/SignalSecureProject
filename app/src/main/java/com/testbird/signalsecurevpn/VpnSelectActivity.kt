package com.testbird.signalsecurevpn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.testbird.signalsecurevpn.util.NetworkUtil
import com.testbird.signalsecurevpn.util.ProjectUtil
import com.testbird.signalsecurevpn.widget.AlertDialogUtil

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
    }

    override fun bindViewId() {
        rv = findViewById(R.id.rv_setting)
        ivBack = findViewById(R.id.iv_back)
        tvTitle = findViewById(R.id.tv_title)
        tvTitle.setText(R.string.setting_location)

        ivBack.setOnClickListener {
            finish()
        }
    }

    override fun getLayout(): Int {
        return R.layout.activity_vpn_select
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setResultForActivity(position: Int) {
        //上次选中的城市
        val lastSelectCity = SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)
        SharePreferenceUtil.putShareString(ProjectUtil.LAST_SELECT_CITY, lastSelectCity)
        //保存选中的VPN 格式：国家-城市
        SharePreferenceUtil.putShareString(ProjectUtil.CUR_SELECT_CITY, NetworkUtil.serviceDataList[position].getName())

        //页面回传值
        val intent = Intent()
        val bundle = Bundle()
        bundle.putInt(ProjectUtil.CUR_SELECT_SERVICE_POSITION,position)
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
}