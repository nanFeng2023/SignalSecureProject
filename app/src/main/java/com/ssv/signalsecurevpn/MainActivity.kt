package com.ssv.signalsecurevpn

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
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
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
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
import com.lzy.okgo.OkGo
import com.lzy.okgo.cache.CacheMode
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.secure.fast.signalvpn.R
import com.ssv.signalsecurevpn.bean.IpTestBean
import com.ssv.signalsecurevpn.bean.VpnBean
import com.ssv.signalsecurevpn.call.FrontAndBackgroundCallBack
import com.ssv.signalsecurevpn.call.TimeDataCallBack
import com.ssv.signalsecurevpn.util.ConfigurationUtil
import com.ssv.signalsecurevpn.util.NetworkUtil
import com.ssv.signalsecurevpn.util.ProjectUtil
import com.ssv.signalsecurevpn.util.TimeUtil
import com.ssv.signalsecurevpn.widget.AlertDialogUtil
import com.ssv.signalsecurevpn.widget.MaskView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
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
//            startConnect()
            detectionIp(true)//IP限制检测，成功后开始连接
        }
    }
    private lateinit var timeDataCallBack: TimeDataCallBack
    private lateinit var onStartConnect: (isStartConnect: Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //必须要写个对象进去切换，才可以正常连接，否则连接失败
        val profile = ProfileManager.getProfile(DataStore.profileId)
            ?: ProfileManager.createProfile(Profile())
        profile.id = DataStore.profileId
        Core.switchProfile(profile.id)
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
            Log.i(
                "TAG",
                "businessProcess()------TimeUtil.curConnectTime:${TimeUtil.curConnectTime}"
            )
            tvConnectTime.text = TimeUtil.curConnectTime
        }
        timeDataCallBack = object : TimeDataCallBack {
            override fun onTime(time: String) {
                tvConnectTime.text = time
            }

            override fun onResetTime() {
                Log.i(
                    "TAG",
                    "onResetTime()------TimeUtil.curConnectTime:${TimeUtil.curConnectTime}"
                )
                tvConnectTime.text = TimeUtil.curConnectTime
            }

        }
        TimeUtil.dataCallList.add(timeDataCallBack)
        //IP检测
        detectionIp(false)

        //Lottie动画设置
        lav.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                Log.i("TAG-MAIN", "onAnimationStart")
                connectOrStopAnimateStart()
            }

            override fun onAnimationEnd(p0: Animator) {
                Log.i("TAG-MAIN", "onAnimationEnd")
                connectOrStopAnimationEnd()
            }

            override fun onAnimationCancel(p0: Animator) {
                Log.i("TAG-MAIN", "onAnimationCancel")

            }

            override fun onAnimationRepeat(p0: Animator) {
//                Log.i("TAG-MAIN", "onAnimationRepeat")
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

        drawer.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

            }

            override fun onDrawerClosed(drawerView: View) {
//                resetOItemCheckState()
            }

            override fun onDrawerStateChanged(newState: Int) {

            }

        })
        //app回到前后台监听
        CustomActivityLifecycleCallback.frontAndBackgroundCallBack =
            object : FrontAndBackgroundCallBack {
                override fun onAppToFront() {
                    ProjectUtil.appToFront = true
                    ProjectUtil.appToBackground = false
                }

                override fun onAppToBackGround() {//比如home键,关闭动画取消,关闭VPN连接
                    Log.i("TAG", "onAppToBackGround()")
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
            SharePreferenceUtil.putShareString(
                ProjectUtil.CUR_SELECT_CITY,
                ProjectUtil.DEFAULT_FAST_SERVERS
            )
        }
        val city = SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)
        ivCountry.setImageResource(
            ProjectUtil.selectCountryIcon(
                ProjectUtil.splitStrGetCountryName(
                    city
                )
            )
        )
        ProjectUtil.isAppMainBack = false

        onStartConnect = {
            //IP限制判断后开始连接
            startConnect()
        }
    }

    private fun resetOItemCheckState() {
        val size = nav.menu.size()
        for (i in 0 until size) {
            val menuItem = nav.menu.getItem(i)
            menuItem.isChecked = false
        }
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
//            ProjectUtil.idle = false
//            ProjectUtil.connected = true
            canClickView()
        } else if (ProjectUtil.stopping) {
            ProjectUtil.stopping = false
//            ProjectUtil.idle = true
//            ProjectUtil.connected = false
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
        Log.i("TAG", "updateVpnInfo()")
        if (currentVpnBean == null)
            return
        Log.i("TAG", "updateVpnInfo()--11")
        val profile = ProfileManager.getProfile(DataStore.profileId)
            ?: ProfileManager.createProfile(Profile())
        profile.host = currentVpnBean?.ip.toString()
        profile.name = "${currentVpnBean?.getName()}"
        profile.method = currentVpnBean?.account.toString()
        profile.password = currentVpnBean?.pwd.toString()
        profile.remotePort = currentVpnBean?.port!!
        ProfileManager.updateProfile(profile)
    }

    private fun jumpConnectResultActivity(isConnected: Boolean) {
        val intent = Intent(this@MainActivity, VpnConnectResultActivity::class.java)
        val city = if (isConnected) {
            SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)
        } else {
            if (ProjectUtil.isVpnSelectPageReqStopVpn) {//VPN选择页面停止VPN
                SharePreferenceUtil.getShareString(ProjectUtil.LAST_SELECT_CITY)
            } else {//主页停止VPN
                SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)
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

    fun snackBar(text: CharSequence = "") =
        Snackbar.make(lav, text, Snackbar.LENGTH_SHORT).apply {
            anchorView = lav
        }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        Log.i("TAG", "stateChanged: state:$state-----profileName:$profileName-----msg:$msg")
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
                jumpConnectResultActivity(true)
            }
            BaseService.State.Stopped -> {//停止成功
                lav.cancelAnimation()
                TimeUtil.pauseTime()//停止计时
                jumpConnectResultActivity(false)
            }
            BaseService.State.Connecting -> {

            }
            BaseService.State.Stopping -> {

            }
        }
    }

    private fun changeVpnState(state: BaseService.State, msg: String? = null) {
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
//                if (ProjectUtil.idle)//解决第一次进入onServiceConnected()回调stopped状态
//                    return
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
            Log.i("TAG", "onServiceConnected:${service.state}")
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        Log.i("TAG", "onPreferenceDataStoreChanged()")
        when (key) {
            Key.serviceMode -> {
                shadowSocksConnection.disconnect(this)
                shadowSocksConnection.connect(this, this)
            }
        }
    }

    override fun onServiceDisconnected() {
        Log.i("TAG", "onServiceDisconnected():state:Idle")
        changeVpnState(BaseService.State.Idle)
        //连接失败
        reqVpnConnectFail()
    }

    override fun onBinderDied() {
        Log.i("TAG", "onBinderDied()")
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
        Log.i("TAG", "stopVpnConnectingAnimation--ProjectUtil.connecting:${ProjectUtil.connecting}")
        if (ProjectUtil.connecting) {//ProjectUtil.connecting后续在动画结束回调里面去重置
            Log.i("TAG", "stopVpnConnectingAnimation--停止")
            lav.cancelAnimation()
            connectionJob?.cancel()
            //取消连接后重置状态
            connectStateChange()
        }
    }

    /*停止VPN关闭动画*/
    private fun stopVpnStoppingAnimation() {
        Log.i("TAG", "stopVpnStoppingAnimation--ProjectUtil.stopping:${ProjectUtil.stopping}")
        if (ProjectUtil.stopping) {
            Log.i("TAG", "stopVpnStoppingAnimation--停止")
            ProjectUtil.stopping = false
            lav.cancelAnimation()
            //取消连接后重置状态
            connectStateChange()
        }
    }

    var connectionJob: Job? = null

    private fun startConnect() {
        if (!netCheck())
            return
        if (ProjectUtil.connecting)//如果正在连接，再次点击不生效
            return
        Log.i("TAG", "startConnect----vpnState:$vpnState")
        if ((vpnState == BaseService.State.Idle && !ProjectUtil.connected)
            || vpnState == BaseService.State.Stopped
        ) {//未连接状态才连接
            Log.i("TAG", "startConnect----开始连接")
            val selectCity = SharePreferenceUtil.getShareString(ProjectUtil.CUR_SELECT_CITY)
            Log.i("TAG", "selectSmartService（）:selectCity:$selectCity")
            if (ProjectUtil.DEFAULT_FAST_SERVERS == selectCity) {//如果是选中的smart则进行测速
                selectSmartService()
            } else {
                //更新profile
                updateVpnInfo()
            }
            lav.repeatMode = LottieDrawable.RESTART
            lav.playAnimation()

            //动画播放5秒后才开始连接VPN 模拟真实请求接口数据
            connectionJob = lifecycleScope.launch {
                Log.i("TAG", "startConnect----开始5秒延时")
                delay(5000)
//                if (ProjectUtil.connecting) {//动画仍然在连接中，才去连接
//                    Core.startService()
//                }
                Log.i("TAG", "startConnect----5秒延时已到")
                Core.startService()
            }
        }
    }

    private fun stopConnect() {
        if (!netCheck())
            return
        if (ProjectUtil.stopping)//如果正在停止，再次点击不生效
            return
        Log.i("TAG", "stopConnect----vpnState:$vpnState")
        if (vpnState.canStop) {//如果VPN连接上才走停止流程
            Log.i("TAG", "stopConnect----停止连接")
            lav.repeatMode = LottieDrawable.REVERSE
            lav.playAnimation()

            //动画播放5秒后才开始关闭VPN 模拟真实请求接口数据
            lav.postDelayed({
                if (ProjectUtil.stopping) {//动画停止中，点击其他按钮后，终止VPN停止过程
                    Core.stopService()//停止VPN
                }
            }, 5000)
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
                    ProjectUtil.callEmail(addresses,this)
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

    /*检测IP*/
    private fun detectionIp(isFromStartConnect: Boolean) {
        val country = Locale.getDefault().country
        val language = Locale.getDefault().language
        Log.i(
            "TAG",
            "MainActivity---detectionIp()---country:$country---language:$language"
        )
        val isRestrictArea = restrictDialog(country)
        if (isRestrictArea && !isFromStartConnect)
            return
        if (!isRestrictArea && isFromStartConnect) {//不是限制地区并且是开始连接时IP检测逻辑，就去连接
            onStartConnect.invoke(true)
            return
        }
        OkGo.get<String>(NetworkUtil.BASE_URL)
            .tag(this)
            .cacheMode(CacheMode.NO_CACHE)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>?) {
                    Log.i("TAG", "success")
                    response?.let { parseData(isFromStartConnect, it.body()) }
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    Log.i("TAG", "error")
//                    val country = Locale.getDefault().country
//                    val language = Locale.getDefault().language
//                    Log.i(
//                        "TAG",
//                        "MainActivity---detectionIp()---country:$country---language:$language"
//                    )
//                    val isRestrictArea = restrictDialog(country)
//                    if (!isRestrictArea && isFromStartConnect) {//不是限制地区并且是开始连接时IP检测逻辑，就去连接
//                        onStartConnect.invoke(true)
//                    }
                }
            })
    }

    private fun parseData(isFromStartConnect: Boolean, data: String) {
        var jsonObj: JSONObject? = null
        try {
            jsonObj = JSONObject(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val country = jsonObj?.opt("country").toString()
        Log.i("TAG", "country:$country")
        val isRestrictArea = restrictDialog(country)
        if (!isRestrictArea && isFromStartConnect) {//不是限制地区并且是开始连接时IP检测逻辑，就去连接
            onStartConnect.invoke(true)
        }
    }

    private fun restrictDialog(country: String?): Boolean {
        Log.i("TAG", "restrictDialog()---当前国家:$country")
        if (NetworkUtil.COUNTRY_HK == country
            /*|| NetworkUtil.COUNTRY_CN == country*/
            || NetworkUtil.COUNTRY_MACAU == country
            || NetworkUtil.COUNTRY_IRAN == country
        ) {
            Log.i("TAG", "restrictDialog()---限制国家:$country")
            AlertDialogUtil().createDialog(
                this, null,
                "Due to the policy reason , this service is not available in your country",
                "confirm",
                { _, _ ->
                    exitProcess(0)
//                    finish()
                }, null, null
            )
            return true
        }
        return false
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

//        ProjectUtil.setStateBarColor(this)
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
    }


    private lateinit var smartCityList: ArrayList<VpnBean>
    private lateinit var ipTestCallBack: ((ip: String, ipDelayTime: Int, count: Int, listSize: Int) -> Unit)
    private lateinit var ipTestList: ArrayList<IpTestBean>
    private lateinit var smartCitySpeedList: ArrayList<VpnBean>

    //测速
    private fun selectSmartService() {
        Log.i("TAG", "selectSmartService（）:开始测速")
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
        var count = 0
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
        if (smartCityList.size>0){
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