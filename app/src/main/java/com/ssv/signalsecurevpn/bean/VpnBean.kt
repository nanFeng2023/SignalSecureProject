package com.ssv.signalsecurevpn.bean

import android.os.Parcel
import android.os.Parcelable

/*服务器VPN列表配置*/
class VpnBean() : Parcelable {
    var account: String? = null
    var country: String? = null
    var pwd: String? = null
    var port: Int = 0
    var city: String? = null
    var ip: String? = null
    var ipDelayTime: Int? = 0

    fun getName(): String? {
        var name: String? = null
        if (country != null) {
            name = country as String
            if (city != null) {
                name = "$country-$city"
            }
        }
        return name
    }

    constructor(parcel: Parcel) : this() {
        account = parcel.readString()
        country = parcel.readString()
        pwd = parcel.readString()
        port = parcel.readInt()
        city = parcel.readString()
        ip = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flag: Int) {
        parcel.writeString(account)
        parcel.writeString(country)
        parcel.writeString(pwd)
        port.let { parcel.writeInt(it) }
        parcel.writeString(city)
        parcel.writeString(ip)
    }

    companion object CREATOR : Parcelable.Creator<VpnBean> {
        override fun createFromParcel(parcel: Parcel): VpnBean {
            return VpnBean(parcel)
        }

        override fun newArray(size: Int): Array<VpnBean?> {
            return arrayOfNulls(size)
        }
    }
}