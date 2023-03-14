package com.ssv.signalsecurevpn

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.secure.fast.signalvpn.R
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.util.ProjectUtil

/*
* vpn选择页面适配器
* */
class SettingAdapter(private val dataList: List<VpnBean>) :
    RecyclerView.Adapter<SettingAdapter.SettingViewHolder>() {

    lateinit var onItemViewClick: (v: View, pos: Int) -> Unit
    private val curSelectName = SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)

    inner class SettingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivCountry: ImageView = view.findViewById(R.id.iv_country_rv_item_setting)
        var tvCountry: TextView = view.findViewById(R.id.tv_country_rv_item_setting)
        var clItem: ConstraintLayout = view.findViewById(R.id.cl_rv_item_setting)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.layout_rv_setting, parent, false)
        return SettingViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        val vpnBean = dataList[position]
        with(holder) {
            tvCountry.text = vpnBean.country
            vpnBean.country
                ?.let { ProjectUtil.selectCountryIcon(it) }
                ?.let { ivCountry.setImageResource(it) }
            selectState(holder, vpnBean)
        }

        //item点击事件
        holder.itemView.setOnClickListener {
            Log.i("TAG", "curSelectName:$curSelectName,getName:${dataList[position].getName()}")
            if ((position == 0 && ProjectUtil.stopped) || curSelectName != dataList[position].getName()) {
                onItemViewClick.invoke(it, position)
            }
        }
    }

    /*选中状态判断*/
    private fun selectState(holder: SettingViewHolder, vpnBean: VpnBean) {
        Log.i(
            "TAG",
            "selectState()---curSelectName:$curSelectName---vpnBean.getName:${vpnBean.getName()}"
        )
        if (curSelectName == vpnBean.getName()) {
            holder.clItem.setBackgroundResource(R.drawable.shape_rv_item_checked_setting)
        } else {
            holder.clItem.setBackgroundResource(R.drawable.shape_rv_item_unchecked_setting)
        }
    }
}