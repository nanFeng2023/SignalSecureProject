package com.ssv.signalsecurevpn.util


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.ssv.signalsecurevpn.App

/*sp帮助类*/
@SuppressLint("StaticFieldLeak")
object SharePreferenceUtil {
    private var sp: SharedPreferences? = null
    private var context: Context = App.appContext
    private fun getSp(context: Context): SharedPreferences {
        if (sp == null) {
            sp = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        }
        return sp!!
    }

    fun putString(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            val edit: SharedPreferences.Editor = getSp(context).edit()
            edit.putString(key, value)
            edit.apply()
        }
    }

    fun getString(key: String): String? {
        if (key.isNotBlank()) {
            val sps: SharedPreferences = getSp(context)
            return sps.getString(key, null)
        }
        return null
    }

    fun putLong(key: String, value: Long) {
        val edit = getSp(context).edit()
        edit.putLong(key, value)
        edit.apply()
    }

    fun getLong(key: String): Long {
        val sps = getSp(context)
        return sps.getLong(key, 0)
    }

    fun putBoolean(key: String, value: Boolean) {
        val edit = getSp(context).edit()
        edit.putBoolean(key, value)
        edit.apply()
    }

    fun getBoolean(key: String,defValue:Boolean): Boolean {
        val sps = getSp(context)
        return sps.getBoolean(key, defValue)
    }
}