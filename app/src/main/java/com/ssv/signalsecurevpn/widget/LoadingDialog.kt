package com.ssv.signalsecurevpn.widget

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.databinding.LayoutLoadDataDialogBinding

class LoadingDialog : DialogFragment() {
    private lateinit var binding: LayoutLoadDataDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = layoutInflater.inflate(R.layout.layout_load_data_dialog, container, false)
        binding = LayoutLoadDataDialogBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    private fun initLayout() {
        context?.let {
            isCancelable = false
            val dialogWidth: Int = dip2px(it, 130f)
            val dialogHeight: Int = dip2px(it, 110f)

            val window = dialog?.window
            val layoutParams = window?.attributes
            layoutParams?.width = dialogWidth
            layoutParams?.height = dialogHeight
            layoutParams?.gravity = Gravity.CENTER_HORIZONTAL
            window?.attributes = layoutParams
        }
    }

    override fun getTheme(): Int {
        return R.style.Theme_dialog
    }

    private fun dip2px(context: Context, dpValue: Float): Int {
        // 获取当前手机的像素密度（1个dp对应几个px）
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt() // 四舍五入取整
    }
}