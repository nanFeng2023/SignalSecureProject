package com.ssv.signalsecurevpn.util

/*配置文件类*/
object ConfigurationUtil {
    const val LOG_TAG = "LOG_TAG"
    const val AD_EXPIRATION_TIME = 4L //广告过期时间

    //-----------------------
    const val PRIVACY_POLICY_URL = ""
    const val MAIL_ACCOUNT = ""
    const val GOOGLE_STORE_URL = "https://play.google.com/store/apps/details?id="

    const val PLAN_PARAM_KEY1 = "sig_home"
    const val PLAN_PARAM_KEY2 = "sig_yes"
    const val PLAN_PARAM_KEY3 = "sig_tio"
    const val PLAN_PARAM_KEY4 = "a_cloak"

    const val REMOTE_AD_KEY = "sigvn_ad"
    const val REMOTE_PLAN_KEY = "sig_vava"

    const val TBA_SERVER_URL = "https://test-lookup.signalvpn.org/chine/grail"

    //该版本不接入cloak
    const val CLOAK_URL = ""

    //下发服务器接口地址
    const val REQ_REMOTE_SERVER_URL = "https://test.signalvpn.org/suQdtPH/ZLqHdRWFR/sGWToLTL/"

    //广告位
    const val AD_SPACE_OPEN = "sigvn_on"
    const val AD_SPACE_NATIVE_HOME = "sigvn_nhome"
    const val AD_SPACE_NATIVE_RESULT = "sigvn_nresult"
    const val AD_SPACE_INTER_CONNECTION = "sigvn_click"
    const val AD_SPACE_INTER_BACK = "sigvn_ib"

    //-----------------------

    const val INSTALL_REFERRER = "install_referrer"
    const val INSTALL_BEGIN_TIME_SECOND = "install_begin_time_second"
    const val CLICK_TIME_SECOND = "click_time_second"
    const val CLICK_TIME_SERVER_SECOND = "click_time_server_second"
    const val INSTALL_BEGIN_TIME_SERVER_SECOND = "install_begin_time_server_second"
    const val INSTALL_VERSION = "install_version"
    const val INSTALL_GOOGLE_PLAY_PARAM = "install_google_play_param"

    const val INSTALL_REFERRER_REQ_OK = "install_referrer_req_ok"

    //打点值
    const val DOT_EXECUTE_PLAN_B = "sig_kuchub"
    const val DOT_VPN_CONNECT = "sig_starv"
    const val DOT_VPN_CONNECT_TIME_STAMP = "sig_enter"
    const val DOT_VPN_PERMISSION_GRANT_USER = "sig_giver"
    const val DOT_VPN_CONNECT_SUCCESS = "sig_eiir"
    const val DOT_VPN_CONNECT_FAIL = "sig_power"
    const val DOT_VPN_CLICK_CONNECT = "sig_ddil"
    const val DOT_VPN_GUIDE_SHOW = "sig_tewuto"
    const val DOT_VPN_GUIDE_CLICK = "sig_guide_min"
    const val DOT_HOME_SHOW = "sig_hatier"
    const val DOT_RESULT_SHOW = "sig_iuper"
    const val DOT_VPN_SERVER_SHOW = "sig_tom"
    const val DOT_LAUNCH_SHOW = "sig_hion"
    const val DOT_REFER_ML_USER = "sig_books"
    const val DOT_PLAN_B_USER_CLICK_VPN_CONNECT = "sig_buyh"
    const val DOT_SERVER_ADDRESS_REQ_INTER_AD = "sig_adcat"
    const val DOT_SERVER_ADDRESS_REQ_RESULT_AD = "sig_adrualt"
    const val DOT_REQ_SERVER_DATA = "sig_kw_e"
    const val DOT_GET_SERVER_DATA = "sig_give_o"
    const val DOT_REQ_SERVER_DATA_TOTAL_TIME = "sig_time_to"

    const val ANDROID_ID = "id"
    const val GAID = "gaid"
    const val LIMIT_TRACK = "limitTrack"

    const val PUBLIC_NETWORK_IP = "networkIp"
    const val CUR_CONNECT_IP = "connectIp"
    const val FAIL_EVENT_SESSION = "failEventSession"
    const val FAIL_EVENT_INSTALL = "failEventInstall"
    const val SESSION_REPORT_TIME = "sessionReportTime"

    const val REMOTE_SERVER_DATA = "remoteServerData"
}