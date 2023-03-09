package com.ssv.signalsecurevpn

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ssv.signalsecurevpn.bean.IpTestBean
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.call.BusinessProcessCallBack
import com.ssv.signalsecurevpn.call.FrontAndBackgroundCallBack
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
import kotlin.system.exitProcess

class MainActivity : BaseActivity(), ShadowsocksConnection.Callback, OnClickListener,
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
        if (it) {//权限拒绝
            snackBar().setText(R.string.vpn_permission_denied).show()
        } else {//权限允许
            startConnect()
        }
    }
    private lateinit var timeDataCallBack: TimeDataCallBack
    var connectionJob: Job? = null//协程

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //必须要写个对象进去切换，才可以正常连接，否则连接失败
        val profile = ProfileManager.getProfile(DataStore.profileId)
            ?: ProfileManager.createProfile(Profile())
        profile.id = DataStore.profileId
        Core.switchProfile(profile.id)
    }

    override fun onResume() {
        super.onResume()
        if (ProjectUtil.isVpnSelectPageBack) {
            AdManager.showAd(this@MainActivity, AdMob.AD_INTER_IB, null)
            AdManager.loadAd(AdMob.AD_INTER_IB, null)
            ProjectUtil.isVpnSelectPageBack = false
        }
    }


    /*业务处理*/
    override fun businessProcess() {
        if (App.isColdLaunch) {//冷启动显示引导页
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

        //Lottie动画设置
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

        //页面回传值  相当于老版的startActivityForResult()
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val bundle = it.data?.extras
                    val position = bundle?.getInt(ProjectUtil.CUR_SELECT_SERVICE_POSITION)
                    currentVpnBean = NetworkUtil.serviceDataList[position!!]
                    //国家图标选择
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

        //app回到前后台监听
        CustomActivityLifecycleCallback.frontAndBackgroundCallBack =
            object : FrontAndBackgroundCallBack {
                override fun onAppToFront() {
                    ProjectUtil.appToFront = true
                    ProjectUtil.appToBackground = false
                }

                override fun onAppToBackGround() {//比如home键,关闭动画取消,关闭VPN连接
                    stopVpnConnectingAnimation()
                    stopVpnStoppingAnimation()
                    ProjectUtil.appToFront = false
                    ProjectUtil.appToBackground = true
                }
            }

        //首次进app默认选择smart
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

        //IP检测
        if (NetworkUtil.isRestrictArea) {
            restrictDialog()
        }

//        loadInterAd()
    }

    private fun loadInterAd() {
        AdManager.loadAd(AdMob.AD_INTER_CLICK, null)
    }

    //开始连接或开始关闭前
    private fun connectOrStopAnimateStart() {
        if (ProjectUtil.idle) {//未连接状态
            ProjectUtil.connecting = true
            canClickView()
        } else if (ProjectUtil.connected) {//连接状态
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

    //请求VPN连接失败
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
                jumpConnectResultActivity(isConnected)
                loadInterAd()
            }

            override fun onAdShowed() {

            }

            override fun onAdShowFail() {
                jumpConnectResultActivity(isConnected)
                loadInterAd()
            }
        })
    }

    private fun jumpConnectResultActivity(isConnected: Boolean) {
        val intent = Intent(this@MainActivity, VpnConnectResultActivity::class.java)
        val city = if (isConnected) {
            SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
        } else {
            if (ProjectUtil.isVpnSelectPageReqStopVpn) {//VPN选择页面停止VPN
                SharePreferenceUtil.getString(ProjectUtil.LAST_SELECT_CITY)
            } else {//主页停止VPN
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

    //VPN状态判断用于计时器和跳转相关逻辑
    private fun judgeVpnConnectState() {
        when (vpnState) {
            BaseService.State.Idle -> {

            }
            BaseService.State.Connected -> {//连接成功
                lav.cancelAnimation()
                //连接成功才开始计时
                TimeUtil.resetTime()
                TimeUtil.startAccumulateTime()
//                jumpConnectResultActivity(true)
                showAd(true)
            }
            BaseService.State.Stopped -> {//停止成功
                lav.cancelAnimation()
                TimeUtil.pauseTime()//停止计时
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
        //状态切换
        when (vpnState) {
            BaseService.State.Idle -> {

            }
            BaseService.State.Connected -> {//连接成功
                ProjectUtil.idle = false
                ProjectUtil.connected = true
                ProjectUtil.stopped = false
                connectStateChange()
            }
            BaseService.State.Stopped -> {//停止成功
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
        //连接失败
        reqVpnConnectFail()
    }

    override fun onBinderDied() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----onBinderDied()")
        shadowSocksConnection.disconnect(this)
        shadowSocksConnection.connect(this, this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.iv_set_main -> {//侧滑菜单栏
                stopVpnStoppingAnimation()
                drawer.open()
            }
            R.id.iv_country_bg_main -> {//vpn选择页面
                stopVpnStoppingAnimation()
                val intent = Intent(this, VpnSelectActivity::class.java)
                activityResultLauncher?.launch(intent)
            }
            R.id.lav_main -> {
                if ((vpnState == BaseService.State.Idle && !ProjectUtil.connected)
                    || vpnState == BaseService.State.Stopped
                ) {//vpn未连接则开始连接
                    connect.launch(null)
                } else if (vpnState == BaseService.State.Connected) {//vpn连接状态点击则断开
                    ProjectUtil.isVpnSelectPageReqStopVpn = false
                    stopConnect()
                }
            }
            R.id.lav_guide_main -> {
                dismissGuideAnimation()
                //开始连接
                connect.launch(null)
            }
        }
    }

    /*停止VPN连接动画*/
    private fun stopVpnConnectingAnimation() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----stopVpnConnectingAnimation()")
        if (ProjectUtil.connecting) {//ProjectUtil.connecting后续在动画结束回调里面去重置
            lav.cancelAnimation()
            connectionJob?.cancel()
            //取消连接后重置状态
            connectStateChange()
        }
    }

    /*停止VPN关闭动画*/
    private fun stopVpnStoppingAnimation() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----stopVpnStoppingAnimation()")
        if (ProjectUtil.stopping) {
            ProjectUtil.stopping = false
            lav.cancelAnimation()
            //取消连接后重置状态
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
                    if (ProjectUtil.connecting)//如果正在连接，再次点击不生效
                        return
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----startConnect()---vpnState:$vpnState")
                    if ((vpnState == BaseService.State.Idle && !ProjectUtil.connected)
                        || vpnState == BaseService.State.Stopped
                    ) {//未连接状态才连接
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----startConnect()---开始连接")
                        val selectCity =
                            SharePreferenceUtil.getString(ProjectUtil.CUR_SELECT_CITY)
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----startConnect()---选择连接城市：$selectCity")
                        if (ProjectUtil.DEFAULT_FAST_SERVERS == selectCity) {//如果是选中的smart则进行测速
                            selectSmartService()
                        } else {
                            //更新profile
                            updateVpnInfo()
                        }

                        //动画播放,开始连接VPN
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
                                    .d("MainActivity----startConnect()---开始连接动画")
                            }.onCompletion {
                                Timber.tag(ConfigurationUtil.LOG_TAG)
                                    .d("MainActivity----startConnect()---开始连接VPN")
                                Core.startService()
                            }.collect {
                                val adAvailable =
                                    AdManager.isAdAvailable(AdMob.AD_INTER_CLICK) ?: false
                                if (adAvailable) {
                                    Timber.tag(ConfigurationUtil.LOG_TAG)
                                        .d("MainActivity----startConnect()---插屏广告有缓存")
                                    connectionJob?.cancel()
                                } else {
                                    Timber.tag(ConfigurationUtil.LOG_TAG)
                                        .d("MainActivity----startConnect()---没有缓存，请求插屏广告")
                                    loadInterAd()
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
        if (ProjectUtil.stopping)//如果正在停止，再次点击不生效
            return
        Timber.tag(ConfigurationUtil.LOG_TAG)
            .d("MainActivity----stopConnect()---vpnState:$vpnState")
        if (vpnState.canStop) {//如果VPN连接上才走停止流程
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
                        .d("MainActivity----stopConnect()---开始关闭动画")
                }.onCompletion {
                    Timber.tag(ConfigurationUtil.LOG_TAG)
                        .d("MainActivity----stopConnect()---开始关闭VPN")
                    if (ProjectUtil.stopping) {//动画停止中，点击其他按钮后，终止VPN停止过程
                        Core.stopService()//停止VPN
                    }
                }.collect {
                    val adAvailable =
                        AdManager.isAdAvailable(AdMob.AD_INTER_CLICK) ?: false
                    if (adAvailable) {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----stopConnect()---插屏广告有缓存")
                        connectionJob?.cancel()
                    } else {
                        Timber.tag(ConfigurationUtil.LOG_TAG)
                            .d("MainActivity----stopConnect()---没有缓存，请求插屏广告")
                        loadInterAd()
                    }
                }
            }
        }
    }

    /*切换连接状态*/
    private fun connectStateChange() {
        if (ProjectUtil.connected) {
            lav.progress = 1f
            ivConnectProgressState.setImageResource(R.mipmap.ic_connect_on_progress_main)
        } else {
            lav.progress = 0f
            ivConnectProgressState.setImageResource(R.mipmap.ic_connect_off_progress_main)
        }
    }

    //nav的menu中item选中监听
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) drawer.closeDrawers() else {
            when (item.itemId) {
                R.id.contract_us_menu -> {
                    val addresses: Array<String> = arrayOf(ConfigurationUtil.MAIL_ACCOUNT)
                    ProjectUtil.callEmail(addresses, this)
                }
                R.id.privacy_policy_menu -> {//隐私政策
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

        //手动刷新状态栏字体颜色深色
        ProjectUtil.setLightStatusBar(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.repeatCount == 0) {//屏蔽返回键
            if (drawer.isOpen) {
                drawer.closeDrawers()
            } else if (ProjectUtil.isShowGuide) {
                dismissGuideAnimation()
            } else if (ProjectUtil.stopping) {
                stopVpnStoppingAnimation()
            } else if (!ProjectUtil.connecting) {
                ProjectUtil.isAppMainBack = true//区分home还是back，决定后续是否走热启动流程
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
    private lateinit var ipTestCallBack: ((ip: String, ipDelayTime: Int, count: Int, listSize: Int) -> Unit)
    private lateinit var ipTestList: ArrayList<IpTestBean>
    private lateinit var smartCitySpeedList: ArrayList<VpnBean>

    //测速
    private fun selectSmartService() {
        Timber.tag(ConfigurationUtil.LOG_TAG).d("MainActivity----selectSmartService()---开始测速")
        smartCityList = ArrayList()
        ipTestList = ArrayList()
        smartCitySpeedList = ArrayList()
        for (i in 0 until NetworkUtil.cityList.size) {
            val cityName: String = NetworkUtil.cityList[i]
            for (j in 0 until NetworkUtil.serviceDataList.size) {
                if (cityName == NetworkUtil.serviceDataList[j].city) {
                    smartCityList.add(NetworkUtil.serviceDataList[j])
                    break
                }
            }
        }
        val count = 0
        //ip测速
        for (i in 0 until smartCityList.size) {
            val vpnBean = smartCityList[i]
            val ip = vpnBean.ip
            ip?.let {
                lifecycleScope.launch {
                    val ipDelay = NetworkUtil.delayTest(it, 2000)
                    ipTestCallBack.invoke(ip, ipDelay, count, smartCityList.size)
                }
            }
        }
        ipTestCallBack = { ip: String, delayTime: Int, countNum: Int, listSize: Int ->
            val bean = IpTestBean()
            bean.ip = ip
            bean.ipDelayTime = delayTime
            ipTestList.add(bean)
            if (countNum == listSize) {//测速完成
                sortIpDelay()
            }
        }
        //随机选择一个
        if (smartCityList.size > 0) {
            val index = Random().nextInt(smartCityList.size)
            currentVpnBean = smartCityList[index]
            //更新profile
            updateVpnInfo()
        }
    }

    //选择排序
    private fun sortIpDelay() {
        for (i in 0 until ipTestList.size - 1) {
            var minIndex = i // 用来记录最小值的索引位置，默认值为i
            for (j in i + 1 until ipTestList.size) {
                if (ipTestList[j].ipDelayTime!! < ipTestList[minIndex].ipDelayTime!!) {
                    minIndex = j // 遍历 i+1~length 的值，找到其中最小值的位置
                }
            }
            // 交换当前索引 i 和最小值索引 minIndex 两处的值
            if (i != minIndex) {
                val temp = ipTestList[i]
                ipTestList[i] = ipTestList[minIndex]
                ipTestList[minIndex] = temp
            }
            // 执行完一次循环，当前索引 i 处的值为最小值，直到循环结束即可完成排序
        }
    }
}