package com.ssv.signalsecurevpn.util

import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.ssv.signalsecurevpn.activity.App
import timber.log.Timber
import kotlin.random.Random

object PlanUtil {
    var isPlanB = false

    private val mlParamList = arrayListOf(
        "【fb4a】", "【gclid】", "【not%20set】", "【youtubeads】", "【%7B%22】"
    )
    private val fbParamList = arrayListOf("【facebook】", "【fb4a】")

    fun obtainReferrer() {
        val isReqInstallOk =
            SharePreferenceUtil.getBoolean(ConfigurationUtil.INSTALL_REFERRER_REQ_OK, false)
        if (isReqInstallOk)
            return
        val referrerClient = InstallReferrerClient.newBuilder(App.appContext).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(p0: Int) {
                Timber.d("obtainReferrer()---code:$p0")
                when (p0) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val referrer = referrerClient.installReferrer
                        val installReferrer = referrer.installReferrer
                        Timber.d("obtainReferrer()---installReferrer:$installReferrer")
                        if (!installReferrer.isNullOrEmpty()) {
                            SharePreferenceUtil.putString(
                                ConfigurationUtil.INSTALL_REFERRER,
                                installReferrer
                            )
                            SharePreferenceUtil.putBoolean(
                                ConfigurationUtil.INSTALL_REFERRER_REQ_OK,
                                true
                            )
                        }

                        val referrerClickTimestampSeconds = referrer.referrerClickTimestampSeconds
                        SharePreferenceUtil.putLong(
                            ConfigurationUtil.CLICK_TIME_SECOND,
                            referrerClickTimestampSeconds
                        )

                        val installBeginTimestampSeconds = referrer.installBeginTimestampSeconds
                        SharePreferenceUtil.putLong(
                            ConfigurationUtil.INSTALL_BEGIN_TIME_SECOND,
                            installBeginTimestampSeconds
                        )

                        val referrerClickTimestampServerSeconds =
                            referrer.referrerClickTimestampServerSeconds
                        SharePreferenceUtil.putLong(
                            ConfigurationUtil.CLICK_TIME_SERVER_SECOND,
                            referrerClickTimestampServerSeconds
                        )

                        val installBeginTimestampServerSeconds =
                            referrer.installBeginTimestampServerSeconds
                        SharePreferenceUtil.putLong(
                            ConfigurationUtil.INSTALL_BEGIN_TIME_SERVER_SECOND,
                            installBeginTimestampServerSeconds
                        )

                        val installVersion = referrer.installVersion
                        if (!installVersion.isNullOrEmpty())
                            SharePreferenceUtil.putString(
                                ConfigurationUtil.INSTALL_VERSION,
                                installVersion
                            )

                        val googlePlayInstantParam = referrer.googlePlayInstantParam
                        SharePreferenceUtil.putBoolean(
                            ConfigurationUtil.INSTALL_GOOGLE_PLAY_PARAM,
                            googlePlayInstantParam
                        )
                    }
                    InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR -> {

                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {

                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED -> {

                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {

                    }
                }
                referrerClient.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                Timber.d("obtainReferrer()---onInstallReferrerServiceDisconnected()---")
            }
        })
    }

    enum class BSwitchType {
        ONLY_COLD_OPEN,
        OPEN,
        CLOSE
    }

    enum class InterType {
        DISPLAY,
        ML_DISPLAY,
        FB_DISPLAY,
        NO_DISPLAY
    }

    enum class UserType {
        ML_USER,
        FB_USER,
        NATURE
    }

    enum class CloakTYPE {
        BLACK_LIST,
        NORMAL
    }

    private fun bType(): BSwitchType {
        val optionResult = NetworkUtil.optionResult
        return when (optionResult.sig_home) {
            "1" -> {
                BSwitchType.ONLY_COLD_OPEN
            }
            "2" -> {
                BSwitchType.OPEN
            }
            else -> {
                BSwitchType.CLOSE
            }
        }
    }

    fun isExecuteB(): Boolean {
        val isExecuteB: Boolean
        val bType = bType()
        isExecuteB = if (bType == BSwitchType.CLOSE) {
            false
        } else {
            val bProbability = isBProbability()
            if (bType == BSwitchType.ONLY_COLD_OPEN) {
                if (App.isColdLaunch) {
                    bProbability
                } else {
                    false
                }
            } else {
                bProbability
            }
        }
        isPlanB = isExecuteB
        return isExecuteB
    }

    private fun isBProbability(): Boolean {
        var probability = 50
        val planRatio = NetworkUtil.optionResult.sig_yes.trim()
        if (planRatio.isNotEmpty()) {
            probability = planRatio.toInt()
        }
        val random = Random.Default
        val randomInt = random.nextInt(101)
        return randomInt <= probability
    }

    fun isShowInterAd(): Boolean {
        when (interShowType()) {
            InterType.DISPLAY -> {
                return true
            }
            InterType.ML_DISPLAY -> {
                val curUserType = curUserType()
                if (curUserType == UserType.ML_USER) {
                    return true
                }
            }
            InterType.FB_DISPLAY -> {
                val curUserType = curUserType()
                if (curUserType == UserType.FB_USER) {
                    return true
                }
            }
            else -> {
                return false
            }
        }
        return false
    }

    fun curUserType(): UserType {
        var userType = UserType.NATURE
        val installUserRef = SharePreferenceUtil.getString(ConfigurationUtil.INSTALL_REFERRER)
        installUserRef?.apply {
            mlParamList.forEach { param ->
                if (installUserRef.contains(param)) {
                    userType = UserType.ML_USER
                }
            }
            fbParamList.forEach { param ->
                if (installUserRef.contains(param)) {
                    userType = UserType.FB_USER
                }
            }
        }
        Timber.d("curUserType()---userType:$userType")
        return userType
    }

    private fun interShowType(): InterType {
        return when (NetworkUtil.optionResult.sig_tio.trim()) {
            "1" -> {
                InterType.DISPLAY
            }
            "2" -> {
                InterType.ML_DISPLAY
            }
            "3" -> {
                InterType.FB_DISPLAY
            }
            else -> {
                InterType.NO_DISPLAY
            }
        }
    }

}