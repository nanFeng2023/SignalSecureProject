package com.ssv.signalsecurevpn

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.snackbar.Snackbar
import com.ssv.signalsecurevpn.ad.AdManager
import com.ssv.signalsecurevpn.ad.AdMob
import com.ssv.signalsecurevpn.ad.AdShowStateCallBack
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.call.BusinessProcessCallBack
import com.ssv.signalsecurevpn.call.FrontAndBackgroundCallBack
import com.ssv.signalsecurevpn.call.IpDelayTestCallBack
import com.ssv.signalsecurevpn.call.TimeDataCallBack
import com.ssv.signalsecurevpn.util.*
import com.ssv.signalsecurevpn.widget.AlertDialogUtil
import com.ssv.signalsecurevpn.widget.MaskView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class MainActivity : BaseActivity(), ShadowsocksConnection.Callback, View.OnClickListener,
    OnPreferenceDataStoreChangeListener,
    OnNavigationItemSelectedListener {
    private lateinit var nav: NavigationView
    private lateinit var drawer: DrawerLayout
    private lateinit var ivSet: ImageView
    private lateinit var ivVpnSelect: ImageView
    private lateinit var lav: LottieAnimationView
    private lateinit var ivConnectProgressState: ImageView
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var ivCountry: ImageView
    private var currentVpnBean: VpnBean? = null
    private lateinit var tvConnectTime: TextView
    private lateinit var lavGuide: LottieAnimationView
    private lateinit var maskView: MaskView
    private lateinit var cl: ConstraintLayout
    private var vpnState = BaseService.State.Idle
    private val shadowSocksConnection = ShadowsocksConnection(true)
    private val connect = registerForActivityResult(PermissionVPN()) {
        if (it) {//????????????
            snackBar().setText(R.string.vpn_permission_denied).show()
        } else {//????????????
            startConnect()
        }
    }
    private lateinit var timeDataCallBack: TimeDataCallBack
    private var connectionJob: Job? = null//??????
    private lateinit var nativeAdViewParentGroup: CardView
    private lateinit var ivAdBg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //??????????????????????????????????????????????????????????????????????????????
        val profile = ProfileManager.getProfile(DataStore.profileId)
            ?: ProfileManager.createProfile(Profile())
        profile.id = DataStore.profileId
        Core.switchProfile(profile.id)
    }

    override fun onResume() {
        super.onResume()
        //????????????????????????
        if (AdMob.isRefreshNativeAd) {
            AdMob.isRefreshNativeAd = false
            Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----onResume()")
            judgeNativeAdShowing()
        }
    }

    private fun judgeNativeAdShowing() {
        if (AdManager.isAdAvailable(AdMob.AD_NATIVE_HOME) == true) {
            ivAdBg.visibility = View.INVISIBLE
            nativeAdViewParentGroup.visibility = View.VISIBLE
            showNativeAd()
        } else {
            ivAdBg.visibility = View.VISIBLE
            nativeAdViewParentGroup.visibility = View.INVISIBLE
            loadNativeHomeAd()
        }
    }

    private fun showNativeAd() {
        //??????????????????
        AdManager.showAd(
            this, AdMob.AD_NATIVE_HOME, object : AdShowStateCallBack {
                override fun onAdDismiss() {

                }

                override fun onAdShowed() {
                    //????????????????????????????????????????????????
                    loadNativeHomeAd()
                }

                override fun onAdShowFail() {

                }

                override fun onAdClicked() {

                }

            },
            R.layout.layout_native_ad_main, nativeAdViewParentGroup
        )
    }

    /*????????????*/
    override fun businessProcess() {
        if (App.isColdLaunch) {//????????????????????????
            App.isColdLaunch = false
            ProjectUtil.isShowGuide = true
            showGuideAnimation()
        }
        if (ProjectUtil.idle) {
            TimeUtil.resetTime()
            tvConnectTime.text = TimeUtil.curConnectTime
        }
        timeDataCallBack = object : TimeDataCallBack {
            override fun onTime(time: String) {
                tvConnectTime.text = time
            }

            override fun onResetTime() {
                tvConnectTime.text = TimeUtil.curConnectTime
            }

        }
        TimeUtil.dataCallList.add(timeDataCallBack)

        //Lottie????????????
        lav.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                connectOrStopAnimateStart()
            }

            override fun onAnimationEnd(p0: Animator) {
                connectOrStopAnimationEnd()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })

        //???????????????  ??????????????????startActivityForResult()
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val bundle = it.data?.extras
                    val position = bundle?.getInt(ProjectUtil.CUR_SELECT_SERVICE_POSITION)
                    currentVpnBean = NetworkUtil.serviceDataList[position!!]
                    //??????????????????
                    currentVpnBean?.country?.let { it1 ->
                        ivCountry.setImageResource(ProjectUtil.selectCountryIcon(it1))
                        if (ProjectUtil.connected) {
                            ProjectUtil.isVpnSelectPageReqStopVpn = true
                            stopConnect()
                        } else {
                            connect.launch(null)
                        }
                    }
                }
            }
        shadowSocksConnection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)

        //app?????????????????????
        CustomActivityLifecycleCallback.frontAndBackgroundCallBack =
            object : FrontAndBackgroundCallBack {
                override fun onAppToFront() {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----onAppToFront()---????????????")
                    ProjectUtil.appToFront = true
                    ProjectUtil.appToBackground = false
                }

                override fun onAppToBackGround() {//??????home???,??????????????????,??????VPN??????
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----onAppToBackGround()---????????????")
                    stopVpnConnectingAnimation()
                    stopVpnStoppingAnimation()
                    ProjectUtil.appToFront = false
                    ProjectUtil.appToBackground = true
                }
            }

        //?????????app????????????smart
        val isFirstIntoApp = SharePreferenceUtil.getBoolean(ProjectUtil.IS_FIRST_INTO_APP, true)
        if (isFirstIntoApp) {
            SharePreferenceUtil.putBoolean(ProjectUtil.IS_FIRST_INTO_APP, false)
            SharePreferenceUtil.putString(
                ProjectUtil.CUR_SELECT_CITY,
                ProjectUtil.DEFAULT_FAST_SERVERS
            )
        }
        val city = SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
        ivCountry.setImageResource(
            ProjectUtil.selectCountryIcon(
                ProjectUtil.splitStrGetCountryName(
                    city
                )
            )
        )
        ProjectUtil.isAppMainBack = false

        //IP??????
        if (NetworkUtil.isRestrictArea) {
            restrictDialog()
        }
    }

    private fun loadNativeResultAd() {
        AdManager.loadAd(AdMob.AD_NATIVE_RESULT, null)
    }

    private fun loadNativeHomeAd() {
        AdManager.loadAd(AdMob.AD_NATIVE_HOME, null)
    }

    private fun loadInterAd() {
        AdManager.loadAd(AdMob.AD_INTER_CLICK, null)
    }

    //??????????????????????????????
    private fun connectOrStopAnimateStart() {
        if (ProjectUtil.idle) {//???????????????
            ProjectUtil.connecting = true
            canClickView()
        } else if (ProjectUtil.connected) {//????????????
            ProjectUtil.stopping = true
        }
    }

    private fun connectOrStopAnimationEnd() {
        if (ProjectUtil.connecting) {
            ProjectUtil.connecting = false
            canClickView()
        } else if (ProjectUtil.stopping) {
            ProjectUtil.stopping = false
        }
    }

    //??????VPN????????????
    private fun reqVpnConnectFail() {
        AlertDialogUtil().createDialog(
            this, null,
            "connection fail",
            "confirm", null, null, null
        )
    }

    private fun canClickView() {
        lav.isClickable = !ProjectUtil.connecting
        ivVpnSelect.isClickable = !ProjectUtil.connecting
        ivSet.isClickable = !ProjectUtil.connecting
    }

    private fun updateVpnInfo() {
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("MainActivity----updateVpnInfo()---currentVpnBean:$currentVpnBean")
        if (currentVpnBean == null)
            return
        val profile = ProfileManager.getProfile(DataStore.profileId)
            ?: ProfileManager.createProfile(Profile())
        profile.host = currentVpnBean?.ip.toString()
        profile.name = "${currentVpnBean?.getName()}"
        profile.method = currentVpnBean?.account.toString()
        profile.password = currentVpnBean?.pwd.toString()
        profile.remotePort = currentVpnBean?.port!!
        ProfileManager.updateProfile(profile)
    }

    private fun showAd(isConnected: Boolean) {
        AdManager.showAd(this@MainActivity, AdMob.AD_INTER_CLICK, object : AdShowStateCallBack {
            override fun onAdDismiss() {
                lifecycleScope.launch {
                    delay(40)
                    jumpConnectResultActivity(isConnected)
                }
                loadInterAd()
            }

            override fun onAdShowed() {

            }

            override fun onAdShowFail() {
                lifecycleScope.launch {
                    delay(40)
                    jumpConnectResultActivity(isConnected)
                }
                loadInterAd()
            }

            override fun onAdClicked() {

            }
        })
    }

    private fun jumpConnectResultActivity(isConnected: Boolean) {
        if (!AdUtil.activityIsResume(this@MainActivity)) {
            if (!isConnected) {//????????????????????????
                TimeUtil.resetTime()
                tvConnectTime.text = TimeUtil.curConnectTime
            }
            return
        }
        val intent = Intent(this@MainActivity, VpnConnectResultActivity::class.java)
        val city = if (isConnected) {
            SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
        } else {
            if (ProjectUtil.isVpnSelectPageReqStopVpn) {//VPN??????????????????VPN
                SharePreferenceUtil.getString(ProjectUtil.LAST_SELECT_CITY)
            } else {//????????????VPN
                SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
            }
        }
        intent.putExtra(ProjectUtil.COUNTRY_KEY,
            city?.let { ProjectUtil.splitStrGetCountryName(it) })
        startActivity(intent)
    }

    override fun bindViewId() {
        nav = findViewById(R.id.nav_main)
        nav.setNavigationItemSelectedListener(this)

        drawer = findViewById(R.id.drawerLayout_main)

        ivSet = findViewById(R.id.iv_set_main)
        ivSet.setOnClickListener(this)

        ivVpnSelect = findViewById(R.id.iv_country_bg_main)
        ivVpnSelect.setOnClickListener(this)

        lav = findViewById(R.id.lav_main)
        lav.setOnClickListener(this)
        ivConnectProgressState = findViewById(R.id.iv_connect_progress_state_main)

        ivCountry = findViewById(R.id.iv_country_main)
        tvConnectTime = findViewById(R.id.tv_connect_time_main)

        lavGuide = findViewById(R.id.lav_guide_main)
        lavGuide.setOnClickListener(this)
        maskView = findViewById(R.id.mask_view_main)
        cl = findViewById(R.id.cl_main)

        nativeAdViewParentGroup = findViewById(R.id.nav_ad_parent_group_main)
        ivAdBg = findViewById(R.id.iv_ad_bg_main)
    }

    override fun getLayout(): Int {
        return R.layout.activity_main
    }

    private fun snackBar(text: CharSequence = "") =
        Snackbar.make(lav, text, Snackbar.LENGTH_SHORT).apply {
            anchorView = lav
        }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----stateChanged()---state:${state}")
        changeVpnState(state)
        judgeVpnConnectState()
    }

    //VPN????????????????????????????????????????????????
    private fun judgeVpnConnectState() {
        when (vpnState) {
            BaseService.State.Idle -> {

            }
            BaseService.State.Connected -> {//????????????
                lav.cancelAnimation()
                //???????????????????????????
                TimeUtil.resetTime()
                TimeUtil.startAccumulateTime()
//                jumpConnectResultActivity(true)
                showAd(true)
            }
            BaseService.State.Stopped -> {//????????????
                lav.cancelAnimation()
                TimeUtil.pauseTime()//????????????
//                jumpConnectResultActivity(false)
                showAd(false)
            }
            BaseService.State.Connecting -> {

            }
            BaseService.State.Stopping -> {

            }
        }
    }

    private fun changeVpnState(state: BaseService.State) {
        this.vpnState = state
        //????????????
        when (vpnState) {
            BaseService.State.Idle -> {

            }
            BaseService.State.Connected -> {//????????????
                ProjectUtil.idle = false
                ProjectUtil.connected = true
                ProjectUtil.stopped = false
                connectStateChange()
            }
            BaseService.State.Stopped -> {//????????????
                ProjectUtil.idle = true
                ProjectUtil.connected = false
                ProjectUtil.stopped = true
                connectStateChange()
            }
            BaseService.State.Connecting -> {

            }
            BaseService.State.Stopping -> {

            }
        }
    }

    override fun onServiceConnected(service: IShadowsocksService) = changeVpnState(
        try {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----onServiceConnected()---state:${service.state}")
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----onPreferenceDataStoreChanged()")
        when (key) {
            Key.serviceMode -> {
                shadowSocksConnection.disconnect(this)
                shadowSocksConnection.connect(this, this)
            }
        }
    }

    override fun onServiceDisconnected() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----onServiceDisconnected()")
        changeVpnState(BaseService.State.Idle)
        //????????????
        reqVpnConnectFail()
    }

    override fun onBinderDied() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----onBinderDied()")
        shadowSocksConnection.disconnect(this)
        shadowSocksConnection.connect(this, this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_set_main -> {//???????????????
                stopVpnStoppingAnimation()
                drawer.open()
            }
            R.id.iv_country_bg_main -> {//vpn????????????
                stopVpnStoppingAnimation()
                val intent = Intent(this, VpnSelectActivity::class.java)
                activityResultLauncher?.launch(intent)
            }
            R.id.lav_main -> {
                if ((vpnState == BaseService.State.Idle && !ProjectUtil.connected)
                    || vpnState == BaseService.State.Stopped
                ) {//vpn????????????????????????
                    connect.launch(null)
                } else if (vpnState == BaseService.State.Connected) {//vpn???????????????????????????
                    ProjectUtil.isVpnSelectPageReqStopVpn = false
                    stopConnect()
                }
            }
            R.id.lav_guide_main -> {
                dismissGuideAnimation()
                //????????????
                connect.launch(null)
            }
        }
    }

    /*??????VPN????????????*/
    private fun stopVpnConnectingAnimation() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----stopVpnConnectingAnimation()")
        if (ProjectUtil.connecting) {//ProjectUtil.connecting??????????????????????????????????????????
            lav.cancelAnimation()
            connectionJob?.cancel()
            //???????????????????????????
            connectStateChange()
        }
    }

    /*??????VPN????????????*/
    private fun stopVpnStoppingAnimation() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----stopVpnStoppingAnimation()")
        if (ProjectUtil.stopping) {
            ProjectUtil.stopping = false
            lav.cancelAnimation()
            //???????????????????????????
            connectStateChange()
            connectionJob?.cancel()
        }
    }

    private fun startConnect() {
        if (!netCheck())
            return
        NetworkUtil.detectionIp(object : BusinessProcessCallBack {
            override fun onBusinessProcess(any: Any?) {
                val isRestrictArea: Boolean
                if (any is Boolean) {
                    isRestrictArea = any
                } else {
                    return
                }

                if (isRestrictArea) {
                    restrictDialog()
                } else {
                    if (ProjectUtil.connecting)//??????????????????????????????????????????
                        return
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----startConnect()---vpnState:$vpnState")
                    if ((vpnState == BaseService.State.Idle && !ProjectUtil.connected)
                        || vpnState == BaseService.State.Stopped
                    ) {//????????????????????????
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----startConnect()---????????????")
                        val selectCity =
                            SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----startConnect()---?????????????????????$selectCity")
                        if (ProjectUtil.DEFAULT_FAST_SERVERS == selectCity) {//??????????????????smart???????????????
                            selectSmartService()
                        } else {
                            //??????profile
                            updateVpnInfo()
                        }

                        //????????????,????????????VPN
                        connectionJob = lifecycleScope.launch {
                            flow {
                                (0 until 10).forEach {
                                    delay(1000)
                                    emit(it)
                                }
                            }.onStart {
                                lav.repeatMode = LottieDrawable.RESTART
                                lav.playAnimation()
                                Timber.tag(ConfigurationUtil.LOG_TAG)
                                    .d("MainActivity----startConnect()---??????????????????")
                                //??????????????????
                                loadInterAd()
                                //???????????????????????????
                                checkNativeResultAd()
                            }.onCompletion {
                                Timber.tag(ConfigurationUtil.LOG_TAG)
                                    .d("MainActivity----startConnect()---????????????VPN")
                                if (ProjectUtil.connecting)
                                    Core.startService()
                            }.collect {
                                val overLimitDay = AdManager.isOverLimitDay()
                                if (overLimitDay == true) {
                                    connectionJob?.cancel()
                                    return@collect
                                }
                                val adAvailable =
                                    AdManager.isAdAvailable(AdMob.AD_INTER_CLICK) ?: false
                                if (adAvailable) {
                                    Timber.tag(ConfigurationUtil.LOG_TAG)
                                        .d("MainActivity----startConnect()---?????????????????????")
                                    connectionJob?.cancel()
                                } else {
                                    Timber.tag(ConfigurationUtil.LOG_TAG)
                                        .d("MainActivity----startConnect()---?????????????????????????????????")
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun stopConnect() {
        if (!netCheck())
            return
        if (ProjectUtil.stopping)//??????????????????????????????????????????
            return
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("MainActivity----stopConnect()---vpnState:$vpnState")
        if (vpnState.canStop) {//??????VPN???????????????????????????
            connectionJob = lifecycleScope.launch {
                flow {
                    (0 until 10).forEach {
                        delay(1000)
                        emit(it)
                    }
                }.onStart {
                    lav.repeatMode = LottieDrawable.REVERSE
                    lav.playAnimation()
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----stopConnect()---??????????????????")
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----stopConnect()---????????????????????????")
                    //????????????????????????
                    loadInterAd()
                    //???????????????????????????
                    checkNativeResultAd()
                }.onCompletion {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----stopConnect()---????????????VPN")
                    if (ProjectUtil.stopping) {//????????????????????????????????????????????????VPN????????????
                        Core.stopService()//??????VPN
                    }
                }.collect {
                    val overLimitDay = AdManager.isOverLimitDay()
                    if (overLimitDay == true) {
                        connectionJob?.cancel()
                        return@collect
                    }
                    val adAvailable =
                        AdManager.isAdAvailable(AdMob.AD_INTER_CLICK) ?: false
                    if (adAvailable) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----stopConnect()---?????????????????????")
                        connectionJob?.cancel()
                    } else {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----stopConnect()---?????????????????????????????????")
                    }
                }
            }
        }
    }

    private fun checkNativeResultAd() {
        if (AdManager.isAdAvailable(AdMob.AD_NATIVE_RESULT) == false) {
            loadNativeResultAd()
        }
    }

    /*??????????????????*/
    private fun connectStateChange() {
        if (ProjectUtil.connected) {
            lav.progress = 1f
            ivConnectProgressState.setImageResource(R.mipmap.ic_connect_on_progress_main)
        } else {
            lav.progress = 0f
            ivConnectProgressState.setImageResource(R.mipmap.ic_connect_off_progress_main)
        }
    }

    //nav???menu???item????????????
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) drawer.closeDrawers() else {
            when (item.itemId) {
                R.id.contract_us_menu -> {
                    val addresses: Array<String> = arrayOf(ConfigurationUtil.MAIL_ACCOUNT)
                    ProjectUtil.callEmail(addresses, this)
                }
                R.id.privacy_policy_menu -> {//????????????
                    startActivity(Intent(this@MainActivity, PrivacyPolicyActivity::class.java))
                }
                R.id.update_menu -> {
                    ProjectUtil.openGooglePlay(this@MainActivity)
                }
                R.id.share_menu -> {
                    ProjectUtil.callShare(this@MainActivity)
                }
            }
        }
        return true
    }


    private fun restrictDialog() {
        AlertDialogUtil().createDialog(
            this, null,
            "Due to the policy reason , this service is not available in your country",
            "confirm",
            { _, _ ->
                exitProcess(0)
            }, null, null
        )
    }

    private fun showGuideAnimation() {
        maskView.visibility = View.VISIBLE
        lavGuide.visibility = View.VISIBLE
        lavGuide.playAnimation()
    }

    private fun dismissGuideAnimation() {
        ProjectUtil.isShowGuide = false
        maskView.visibility = View.GONE
        lavGuide.visibility = View.GONE
        lavGuide.cancelAnimation()

        //???????????????????????????????????????
        ProjectUtil.setLightStatusBar(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.repeatCount == 0) {//???????????????
            if (drawer.isOpen) {
                drawer.closeDrawers()
            } else if (ProjectUtil.isShowGuide) {
                dismissGuideAnimation()
            } else if (ProjectUtil.stopping) {
                stopVpnStoppingAnimation()
            } else if (!ProjectUtil.connecting) {
                ProjectUtil.isAppMainBack = true//??????home??????back???????????????????????????????????????
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        shadowSocksConnection.disconnect(this)
        TimeUtil.dataCallList.remove(timeDataCallBack)
        CallBackUtil.businessProcessCallBack = null
    }

    private lateinit var smartCityList: ArrayList<VpnBean>

    //smart??????
    private fun selectSmartService() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----selectSmartService()---????????????")
        smartCityList = ArrayList()
        if (NetworkUtil.cityList.size > 0) {//smart???????????????
            for (i in 0 until NetworkUtil.cityList.size) {
                val cityName: String = NetworkUtil.cityList[i]
                for (j in 0 until NetworkUtil.serviceDataList.size) {
                    if (cityName == NetworkUtil.serviceDataList[j].city) {
                        smartCityList.add(NetworkUtil.serviceDataList[j])
                        break
                    }
                }
            }
        }

        val smartListSize = smartCityList.size
        if (smartListSize == 0) {//???????????????????????????smart???????????????????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----selectSmartService()---?????????????????????smart????????????????????????????????????????????????")
            smartCityList.addAll(NetworkUtil.serviceDataList)
            if (smartCityList.size > 0)//???????????????smart?????????????????????
                smartCityList.removeAt(0)
            sortIpDelayTime()
        } else if (smartListSize <= 3) {//??????smart?????????????????????3,????????????????????????
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----selectSmartService()---smart?????????????????????3,????????????????????????")
            randomSelectVpnAndUpdateInfo(smartCityList)
        } else if (smartListSize > 3) {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----selectSmartService()---smart?????????????????????3,???????????????????????????????????????????????????????????????????????????")
            sortIpDelayTime()
        }
    }

    private fun sortIpDelayTime() {
        var count = 0
        //ip??????
        for (i in 0 until smartCityList.size) {
            val vpnBean = smartCityList[i]
            val ip = vpnBean.ip
            ip?.let {
                lifecycleScope.launch {
                    NetworkUtil.delayTest(vpnBean, object : IpDelayTestCallBack {
                        override fun onIpDelayTest(vpnBean: VpnBean, ipDelayTime: Int) {
                            Timber.tag(ConfigurationUtil.LOG_TAG)
                                .d("MainActivity----selectSmartService()---onIpDelayTest()---ip:${vpnBean.ip}---ip????????????:$ipDelayTime")
                            vpnBean.ipDelayTime = ipDelayTime
                            count++
                            if (count == smartCityList.size) {
                                smartCityList.sortBy {
                                    it.ipDelayTime
                                }
                                val randomVpnList: ArrayList<VpnBean> = ArrayList()
                                for (j in 0 until 3) {
                                    randomVpnList.add(smartCityList[j])
                                }
                                randomSelectVpnAndUpdateInfo(randomVpnList)
                            }
                        }
                    })
                }
            }
        }
    }

    private fun randomSelectVpnAndUpdateInfo(list: ArrayList<VpnBean>) {
        if (list.size > 0) {
            val index = Random().nextInt(list.size)
            currentVpnBean = list[index]
            //??????profile
            updateVpnInfo()
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----randomSelectVpnAndUpdateInfo()---?????????IP???${currentVpnBean!!.ip}")
        } else {
            Timber.tag(ConfigurationUtil.LOG_TAG)
                .d("MainActivity----randomSelectVpnAndUpdateInfo()---list???????????????")
        }
    }

}