package com.ssv.signalsecurevpn.activity

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import com.ssv.signalsecurevpn.R
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.ProjectUtil

/*隐私政策页面*/
class PrivacyPolicyActivity : BaseActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun businessProcess() {
        val drawable = ivBack.drawable
        drawable.setColorFilter(
            resources.getColor(R.color.connect_fail_color_result),
            PorterDuff.Mode.SRC_ATOP
        )
        //设置深色状态栏文字
        ProjectUtil.setLightStatusBar(this)
        //初始化webView配置
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl(ConfigurationUtil.PRIVACY_POLICY_URL)
    }

    override fun bindViewId() {
        ivBack = findViewById(R.id.iv_back)
        ivBack.setOnClickListener {
            finish()
        }
        webView = findViewById(R.id.web_privacy)
    }

    override fun getLayout(): Int {
        return R.layout.activity_privacy_policy
    }
}