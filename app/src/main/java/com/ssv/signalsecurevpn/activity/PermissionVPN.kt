package com.ssv.signalsecurevpn.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContract
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Key
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.FirebaseUtils
import timber.log.Timber

class PermissionVPN : ActivityResultContract<Void?, Boolean>() {
    private var cachedIntent: Intent? = null

    override fun getSynchronousResult(context: Context, input: Void?): SynchronousResult<Boolean>? {
        if (DataStore.serviceMode == Key.modeVpn) VpnService.prepare(context)?.let { intent ->
            cachedIntent = intent
            return null
        }
        return SynchronousResult(false)
    }

    override fun createIntent(context: Context, input: Void?) =
        cachedIntent!!.also { cachedIntent = null }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == Activity.RESULT_OK) {
            FirebaseUtils.upLoadLogEvent(ConfigurationUtil.DOT_VPN_PERMISSION_GRANT_USER)
            false
        } else {
            Timber.e("Failed to start VpnService: $intent")
            true
        }
}